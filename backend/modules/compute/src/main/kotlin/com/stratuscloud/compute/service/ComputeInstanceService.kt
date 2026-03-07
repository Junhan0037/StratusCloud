package com.stratuscloud.compute.service

import com.stratuscloud.compute.domain.ComputeAutoscalingGroupEntity
import com.stratuscloud.compute.domain.ComputeHealthStatus
import com.stratuscloud.compute.domain.ComputeImageStatus
import com.stratuscloud.compute.domain.ComputeInstanceEntity
import com.stratuscloud.compute.domain.ComputeInstanceStatus
import com.stratuscloud.compute.repository.ComputeInstanceRepository
import com.stratuscloud.iam.exception.BadRequestException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import com.stratuscloud.iam.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ComputeInstanceService(
    private val computeInstanceRepository: ComputeInstanceRepository,
    private val computeImageService: ComputeImageService,
    private val projectRepository: ProjectRepository
) {

    @Transactional
    fun createInstance(
        tenantId: UUID,
        projectId: UUID,
        name: String,
        imageId: UUID,
        flavor: String,
        userData: String?,
        actorId: UUID,
        autoscalingGroupId: UUID? = null
    ): ComputeInstanceEntity {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ResourceNotFoundException("project not found: $projectId") }
        if (project.tenantId != tenantId) {
            throw BadRequestException("project does not belong to tenant: $projectId")
        }

        val image = computeImageService.getImage(imageId)
        if (image.tenantId != tenantId) {
            throw BadRequestException("image does not belong to tenant: $imageId")
        }
        if (image.status != ComputeImageStatus.ACTIVE) {
            throw BadRequestException("image is not active: $imageId")
        }

        val normalizedFlavor = normalizeFlavor(flavor)
        val instance = computeInstanceRepository.save(
            ComputeInstanceEntity(
                tenantId = tenantId,
                projectId = projectId,
                imageId = imageId,
                autoscalingGroupId = autoscalingGroupId,
                name = name.trim(),
                flavor = normalizedFlavor,
                status = ComputeInstanceStatus.PENDING,
                userData = userData?.trim()?.takeIf { it.isNotBlank() },
                healthStatus = ComputeHealthStatus.UNKNOWN,
                restartCount = 0,
                lastTransitionAt = LocalDateTime.now(),
                createdBy = actorId.toString()
            )
        )
        instance.status = ComputeInstanceStatus.RUNNING
        instance.lastTransitionAt = LocalDateTime.now()
        return computeInstanceRepository.save(instance)
    }

    @Transactional(readOnly = true)
    fun listInstances(tenantId: UUID, projectId: UUID): List<ComputeInstanceEntity> {
        return computeInstanceRepository.findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId)
            .filter { it.status != ComputeInstanceStatus.TERMINATED }
    }

    @Transactional(readOnly = true)
    fun getInstance(instanceId: UUID): ComputeInstanceEntity {
        return computeInstanceRepository.findById(instanceId)
            .orElseThrow { ResourceNotFoundException("compute instance not found: $instanceId") }
    }

    @Transactional(readOnly = true)
    fun listGroupInstances(groupId: UUID): List<ComputeInstanceEntity> {
        return computeInstanceRepository.findAllByAutoscalingGroupIdOrderByCreatedAtDesc(groupId)
    }

    @Transactional(readOnly = true)
    fun listActiveGroupInstances(groupId: UUID): List<ComputeInstanceEntity> {
        return listGroupInstances(groupId).filter { it.status != ComputeInstanceStatus.TERMINATED }
    }

    @Transactional
    fun createGroupManagedInstance(
        group: ComputeAutoscalingGroupEntity,
        actorId: UUID,
        sequence: Int
    ): ComputeInstanceEntity {
        return createInstance(
            tenantId = group.tenantId,
            projectId = group.projectId,
            name = "${group.name}-$sequence",
            imageId = group.imageId,
            flavor = group.flavor,
            userData = null,
            actorId = actorId,
            autoscalingGroupId = requireNotNull(group.id) { "autoscaling group id is null" }
        )
    }

    @Transactional
    fun startInstance(instanceId: UUID): ComputeInstanceEntity {
        val instance = getInstance(instanceId)
        requireTransition(instance, allowedCurrent = setOf(ComputeInstanceStatus.STOPPED), next = ComputeInstanceStatus.RUNNING)
        return saveTransition(instance, ComputeInstanceStatus.RUNNING)
    }

    @Transactional
    fun stopInstance(instanceId: UUID): ComputeInstanceEntity {
        val instance = getInstance(instanceId)
        requireTransition(instance, allowedCurrent = setOf(ComputeInstanceStatus.RUNNING), next = ComputeInstanceStatus.STOPPED)
        return saveTransition(instance, ComputeInstanceStatus.STOPPED)
    }

    @Transactional
    fun terminateInstance(instanceId: UUID): ComputeInstanceEntity {
        val instance = getInstance(instanceId)
        requireTransition(
            instance,
            allowedCurrent = setOf(ComputeInstanceStatus.RUNNING, ComputeInstanceStatus.STOPPED),
            next = ComputeInstanceStatus.TERMINATED
        )
        return saveTransition(instance, ComputeInstanceStatus.TERMINATED)
    }

    @Transactional
    fun updateHealthStatus(instanceId: UUID, healthStatus: ComputeHealthStatus): ComputeInstanceEntity {
        val instance = getInstance(instanceId)
        instance.healthStatus = healthStatus
        return computeInstanceRepository.save(instance)
    }

    @Transactional
    fun restartInstance(instanceId: UUID): ComputeInstanceEntity {
        val instance = getInstance(instanceId)
        if (instance.status == ComputeInstanceStatus.TERMINATED) {
            throw BadRequestException("cannot restart terminated instance: $instanceId")
        }
        instance.status = ComputeInstanceStatus.RUNNING
        instance.healthStatus = ComputeHealthStatus.HEALTHY
        instance.restartCount += 1
        instance.lastTransitionAt = LocalDateTime.now()
        return computeInstanceRepository.save(instance)
    }

    fun validateFlavor(flavor: String): String {
        return normalizeFlavor(flavor)
    }

    private fun saveTransition(instance: ComputeInstanceEntity, next: ComputeInstanceStatus): ComputeInstanceEntity {
        instance.status = next
        instance.lastTransitionAt = LocalDateTime.now()
        return computeInstanceRepository.save(instance)
    }

    private fun requireTransition(
        instance: ComputeInstanceEntity,
        allowedCurrent: Set<ComputeInstanceStatus>,
        next: ComputeInstanceStatus
    ) {
        if (instance.status !in allowedCurrent) {
            throw BadRequestException("cannot transition instance from ${instance.status} to $next")
        }
    }

    private fun normalizeFlavor(flavor: String): String {
        val normalized = flavor.trim().lowercase()
        if (normalized !in SUPPORTED_FLAVORS) {
            throw BadRequestException("unsupported flavor: $flavor")
        }
        return normalized
    }

    companion object {
        private val SUPPORTED_FLAVORS = setOf("nano", "small", "medium")
    }
}
