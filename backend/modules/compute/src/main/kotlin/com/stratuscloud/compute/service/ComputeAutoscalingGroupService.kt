package com.stratuscloud.compute.service

import com.stratuscloud.compute.domain.ComputeAutoscalingGroupEntity
import com.stratuscloud.compute.domain.ComputeAutoscalingGroupStatus
import com.stratuscloud.compute.domain.ComputeHealthPolicy
import com.stratuscloud.compute.domain.ComputeImageStatus
import com.stratuscloud.compute.domain.ComputeInstanceEntity
import com.stratuscloud.compute.domain.ComputeInstanceMetricEntity
import com.stratuscloud.compute.repository.ComputeAutoscalingGroupRepository
import com.stratuscloud.compute.repository.ComputeInstanceMetricRepository
import com.stratuscloud.iam.exception.BadRequestException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import com.stratuscloud.iam.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class ComputeAutoscalingEvaluation(
    val group: ComputeAutoscalingGroupEntity,
    val currentInstances: Int,
    val averageCpuPercent: Int?,
    val averageMemoryPercent: Int?
)

@Service
class ComputeAutoscalingGroupService(
    private val computeAutoscalingGroupRepository: ComputeAutoscalingGroupRepository,
    private val computeInstanceMetricRepository: ComputeInstanceMetricRepository,
    private val computeInstanceService: ComputeInstanceService,
    private val computeImageService: ComputeImageService,
    private val projectRepository: ProjectRepository
) {

    @Transactional
    fun createGroup(
        tenantId: UUID,
        projectId: UUID,
        name: String,
        imageId: UUID,
        flavor: String,
        minInstances: Int,
        maxInstances: Int,
        cpuScaleOutPercent: Int,
        cpuScaleInPercent: Int,
        memoryScaleOutPercent: Int,
        memoryScaleInPercent: Int,
        healthPolicy: ComputeHealthPolicy,
        failureThreshold: Int,
        actorId: UUID
    ): ComputeAutoscalingEvaluation {
        validateProject(tenantId, projectId)
        validateImage(tenantId, imageId)
        computeInstanceService.validateFlavor(flavor)
        validateScalingBounds(minInstances, maxInstances, cpuScaleOutPercent, cpuScaleInPercent, memoryScaleOutPercent, memoryScaleInPercent)
        if (failureThreshold < 1) {
            throw BadRequestException("failureThreshold must be greater than 0")
        }

        val saved = computeAutoscalingGroupRepository.save(
            ComputeAutoscalingGroupEntity(
                tenantId = tenantId,
                projectId = projectId,
                name = name.trim(),
                imageId = imageId,
                flavor = flavor.trim().lowercase(),
                minInstances = minInstances,
                maxInstances = maxInstances,
                desiredInstances = minInstances,
                cpuScaleOutPercent = cpuScaleOutPercent,
                cpuScaleInPercent = cpuScaleInPercent,
                memoryScaleOutPercent = memoryScaleOutPercent,
                memoryScaleInPercent = memoryScaleInPercent,
                healthPolicy = healthPolicy,
                failureThreshold = failureThreshold,
                status = ComputeAutoscalingGroupStatus.ACTIVE,
                createdBy = actorId.toString()
            )
        )
        reconcileDesired(saved, actorId)
        return describe(requireNotNull(saved.id))
    }

    @Transactional(readOnly = true)
    fun listGroups(tenantId: UUID, projectId: UUID): List<ComputeAutoscalingEvaluation> {
        validateProject(tenantId, projectId)
        return computeAutoscalingGroupRepository.findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId)
            .map { describe(requireNotNull(it.id)) }
    }

    @Transactional(readOnly = true)
    fun getGroup(groupId: UUID): ComputeAutoscalingGroupEntity {
        return computeAutoscalingGroupRepository.findById(groupId)
            .orElseThrow { ResourceNotFoundException("compute autoscaling group not found: $groupId") }
    }

    @Transactional(readOnly = true)
    fun describe(groupId: UUID): ComputeAutoscalingEvaluation {
        val group = getGroup(groupId)
        val activeInstances = computeInstanceService.listActiveGroupInstances(groupId)
        val metrics = latestMetrics(activeInstances)
        return ComputeAutoscalingEvaluation(
            group = group,
            currentInstances = activeInstances.size,
            averageCpuPercent = metrics.map { it.cpuPercent }.averageOrNull(),
            averageMemoryPercent = metrics.map { it.memoryPercent }.averageOrNull()
        )
    }

    @Transactional
    fun evaluate(groupId: UUID, actorId: UUID): ComputeAutoscalingEvaluation {
        val group = getGroup(groupId)
        val activeInstances = computeInstanceService.listActiveGroupInstances(groupId)
        val metrics = latestMetrics(activeInstances)
        if (metrics.isNotEmpty()) {
            val averageCpu = metrics.map { it.cpuPercent }.average().toInt()
            val averageMemory = metrics.map { it.memoryPercent }.average().toInt()
            when {
                averageCpu >= group.cpuScaleOutPercent || averageMemory >= group.memoryScaleOutPercent -> {
                    group.desiredInstances = (group.desiredInstances + 1).coerceAtMost(group.maxInstances)
                }
                averageCpu < group.cpuScaleInPercent && averageMemory < group.memoryScaleInPercent -> {
                    group.desiredInstances = (group.desiredInstances - 1).coerceAtLeast(group.minInstances)
                }
            }
        }
        computeAutoscalingGroupRepository.save(group)
        reconcileDesired(group, actorId)
        return describe(groupId)
    }

    @Transactional
    fun reconcileDesired(group: ComputeAutoscalingGroupEntity, actorId: UUID): List<ComputeInstanceEntity> {
        val groupId = requireNotNull(group.id) { "autoscaling group id is null" }
        val activeInstances = computeInstanceService.listActiveGroupInstances(groupId).toMutableList()
        if (activeInstances.size < group.desiredInstances) {
            val nextSequenceStart = computeInstanceService.listGroupInstances(groupId).size + 1
            val missing = group.desiredInstances - activeInstances.size
            repeat(missing) { index ->
                activeInstances += computeInstanceService.createGroupManagedInstance(group, actorId, nextSequenceStart + index)
            }
        } else if (activeInstances.size > group.desiredInstances) {
            val removable = activeInstances
                .sortedByDescending { it.createdAt }
                .take(activeInstances.size - group.desiredInstances)
            removable.forEach { computeInstanceService.terminateInstance(requireNotNull(it.id)) }
        }
        return computeInstanceService.listActiveGroupInstances(groupId)
    }

    private fun validateProject(tenantId: UUID, projectId: UUID) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ResourceNotFoundException("project not found: $projectId") }
        if (project.tenantId != tenantId) {
            throw BadRequestException("project does not belong to tenant: $projectId")
        }
    }

    private fun validateImage(tenantId: UUID, imageId: UUID) {
        val image = computeImageService.getImage(imageId)
        if (image.tenantId != tenantId) {
            throw BadRequestException("image does not belong to tenant: $imageId")
        }
        if (image.status != ComputeImageStatus.ACTIVE) {
            throw BadRequestException("image is not active: $imageId")
        }
    }

    private fun validateScalingBounds(
        minInstances: Int,
        maxInstances: Int,
        cpuScaleOutPercent: Int,
        cpuScaleInPercent: Int,
        memoryScaleOutPercent: Int,
        memoryScaleInPercent: Int
    ) {
        if (minInstances < 1) {
            throw BadRequestException("minInstances must be greater than 0")
        }
        if (maxInstances < minInstances) {
            throw BadRequestException("maxInstances must be greater than or equal to minInstances")
        }
        if (cpuScaleInPercent >= cpuScaleOutPercent) {
            throw BadRequestException("cpuScaleInPercent must be lower than cpuScaleOutPercent")
        }
        if (memoryScaleInPercent >= memoryScaleOutPercent) {
            throw BadRequestException("memoryScaleInPercent must be lower than memoryScaleOutPercent")
        }
    }

    private fun latestMetrics(instances: List<ComputeInstanceEntity>): List<ComputeInstanceMetricEntity> {
        val instanceIds = instances.mapNotNull { it.id }
        if (instanceIds.isEmpty()) {
            return emptyList()
        }
        return computeInstanceMetricRepository.findAllByInstanceIdIn(instanceIds)
    }

    private fun List<Int>.averageOrNull(): Int? {
        if (isEmpty()) {
            return null
        }
        return average().toInt()
    }
}
