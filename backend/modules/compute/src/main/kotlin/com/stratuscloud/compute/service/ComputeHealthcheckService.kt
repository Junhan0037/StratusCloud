package com.stratuscloud.compute.service

import com.stratuscloud.compute.domain.ComputeHealthStatus
import com.stratuscloud.compute.domain.ComputeInstanceHealthCheckEntity
import com.stratuscloud.compute.domain.ComputeInstanceStatus
import com.stratuscloud.compute.repository.ComputeInstanceHealthCheckRepository
import com.stratuscloud.iam.exception.BadRequestException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

data class ComputeHealthReconcileResult(
    val restartedInstanceIds: List<UUID>,
    val replacementInstanceIds: List<UUID>
)

@Service
class ComputeHealthcheckService(
    private val computeInstanceHealthCheckRepository: ComputeInstanceHealthCheckRepository,
    private val computeInstanceService: ComputeInstanceService,
    private val computeAutoscalingGroupService: ComputeAutoscalingGroupService
) {

    @Transactional
    fun reportHealth(
        instanceId: UUID,
        status: ComputeHealthStatus,
        detail: String?
    ): ComputeInstanceHealthCheckEntity {
        val instance = computeInstanceService.getInstance(instanceId)
        if (instance.status == ComputeInstanceStatus.TERMINATED) {
            throw BadRequestException("cannot report health for terminated instance: $instanceId")
        }
        val current = computeInstanceHealthCheckRepository.findByInstanceId(instanceId)
            ?: ComputeInstanceHealthCheckEntity(instanceId = instanceId)
        current.status = status
        current.detail = detail?.trim()?.takeIf { it.isNotBlank() }
        current.checkedAt = LocalDateTime.now()
        current.failureCount = if (status == ComputeHealthStatus.UNHEALTHY) current.failureCount + 1 else 0
        val saved = computeInstanceHealthCheckRepository.save(current)
        computeInstanceService.updateHealthStatus(instanceId, status)
        return saved
    }

    @Transactional
    fun reconcileGroup(groupId: UUID): ComputeHealthReconcileResult {
        val group = computeAutoscalingGroupService.getGroup(groupId)
        val restartedInstanceIds = mutableListOf<UUID>()
        val replacementInstanceIds = mutableListOf<UUID>()
        val activeInstances = computeInstanceService.listActiveGroupInstances(groupId)
        activeInstances.forEach { instance ->
            val instanceId = requireNotNull(instance.id) { "instance id is null" }
            val healthCheck = computeInstanceHealthCheckRepository.findByInstanceId(instanceId) ?: return@forEach
            if (healthCheck.status != ComputeHealthStatus.UNHEALTHY || healthCheck.failureCount < group.failureThreshold) {
                return@forEach
            }
            val restarted = computeInstanceService.restartInstance(instanceId)
            restartedInstanceIds += requireNotNull(restarted.id)
            healthCheck.status = ComputeHealthStatus.HEALTHY
            healthCheck.failureCount = 0
            healthCheck.detail = "restarted"
            healthCheck.checkedAt = LocalDateTime.now()
            computeInstanceHealthCheckRepository.save(healthCheck)
        }
        return ComputeHealthReconcileResult(
            restartedInstanceIds = restartedInstanceIds,
            replacementInstanceIds = replacementInstanceIds
        )
    }
}
