package com.stratuscloud.compute.service

import com.stratuscloud.compute.domain.ComputeInstanceMetricEntity
import com.stratuscloud.compute.repository.ComputeInstanceMetricRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ComputeInstanceTelemetryService(
    private val computeInstanceMetricRepository: ComputeInstanceMetricRepository,
    private val computeInstanceService: ComputeInstanceService
) {

    @Transactional
    fun writeMetric(
        instanceId: UUID,
        cpuPercent: Int,
        memoryPercent: Int,
        actorId: UUID
    ): ComputeInstanceMetricEntity {
        computeInstanceService.getInstance(instanceId)
        val metric = computeInstanceMetricRepository.findByInstanceId(instanceId)
            ?: ComputeInstanceMetricEntity(instanceId = instanceId)
        metric.cpuPercent = cpuPercent
        metric.memoryPercent = memoryPercent
        metric.reportedAt = LocalDateTime.now()
        metric.reportedBy = actorId.toString()
        return computeInstanceMetricRepository.save(metric)
    }

    @Transactional(readOnly = true)
    fun latestMetrics(instanceIds: Collection<UUID>): Map<UUID, ComputeInstanceMetricEntity> {
        if (instanceIds.isEmpty()) {
            return emptyMap()
        }
        return computeInstanceMetricRepository.findAllByInstanceIdIn(instanceIds).associateBy { it.instanceId }
    }
}
