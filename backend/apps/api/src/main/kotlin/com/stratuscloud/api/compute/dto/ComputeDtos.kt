package com.stratuscloud.api.compute.dto

import com.stratuscloud.compute.domain.ComputeAutoscalingGroupEntity
import com.stratuscloud.compute.domain.ComputeAutoscalingGroupStatus
import com.stratuscloud.compute.domain.ComputeHealthPolicy
import com.stratuscloud.compute.domain.ComputeHealthStatus
import com.stratuscloud.compute.domain.ComputeImageEntity
import com.stratuscloud.compute.domain.ComputeImageStatus
import com.stratuscloud.compute.domain.ComputeInstanceEntity
import com.stratuscloud.compute.domain.ComputeInstanceHealthCheckEntity
import com.stratuscloud.compute.domain.ComputeInstanceMetricEntity
import com.stratuscloud.compute.domain.ComputeInstanceStatus
import com.stratuscloud.compute.domain.ComputeOsType
import com.stratuscloud.compute.service.ComputeAutoscalingEvaluation
import com.stratuscloud.compute.service.ComputeHealthReconcileResult
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateComputeImageRequest(
    val tenantId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    @field:NotBlank
    @field:Size(min = 1, max = 50)
    val version: String,
    val osType: ComputeOsType,
    val status: ComputeImageStatus? = null,
    val tags: List<String> = emptyList()
)

data class ComputeImageResponse(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val version: String,
    val osType: ComputeOsType,
    val status: ComputeImageStatus,
    val tags: List<String>,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: ComputeImageEntity): ComputeImageResponse {
            return ComputeImageResponse(
                id = entity.id ?: error("compute image id is null"),
                tenantId = entity.tenantId,
                name = entity.name,
                version = entity.version,
                osType = entity.osType,
                status = entity.status,
                tags = entity.tags.split(",").mapNotNull { value ->
                    value.trim().takeIf { it.isNotBlank() }
                },
                createdAt = entity.createdAt
            )
        }
    }
}

data class CreateComputeInstanceRequest(
    val tenantId: UUID,
    val projectId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    val imageId: UUID,
    @field:NotBlank
    @field:Size(min = 4, max = 20)
    val flavor: String,
    @field:Size(max = 8192)
    val userData: String? = null
)

data class ComputeInstanceResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val imageId: UUID,
    val autoscalingGroupId: UUID?,
    val name: String,
    val flavor: String,
    val status: ComputeInstanceStatus,
    val healthStatus: ComputeHealthStatus,
    val restartCount: Int,
    val latestMetric: ComputeInstanceMetricResponse?,
    val userData: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val lastTransitionAt: LocalDateTime
) {
    companion object {
        fun from(
            entity: ComputeInstanceEntity,
            metric: ComputeInstanceMetricEntity? = null
        ): ComputeInstanceResponse {
            return ComputeInstanceResponse(
                id = entity.id ?: error("compute instance id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                imageId = entity.imageId,
                autoscalingGroupId = entity.autoscalingGroupId,
                name = entity.name,
                flavor = entity.flavor,
                status = entity.status,
                healthStatus = entity.healthStatus,
                restartCount = entity.restartCount,
                latestMetric = metric?.let { ComputeInstanceMetricResponse.from(it) },
                userData = entity.userData,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                lastTransitionAt = entity.lastTransitionAt
            )
        }
    }
}

data class CreateComputeAutoscalingGroupRequest(
    val tenantId: UUID,
    val projectId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    val imageId: UUID,
    @field:NotBlank
    @field:Size(min = 4, max = 20)
    val flavor: String,
    @field:Min(1)
    val minInstances: Int,
    @field:Min(1)
    val maxInstances: Int,
    @field:Min(1)
    @field:Max(100)
    val cpuScaleOutPercent: Int,
    @field:Min(1)
    @field:Max(100)
    val cpuScaleInPercent: Int,
    @field:Min(1)
    @field:Max(100)
    val memoryScaleOutPercent: Int,
    @field:Min(1)
    @field:Max(100)
    val memoryScaleInPercent: Int,
    val healthPolicy: ComputeHealthPolicy? = null,
    @field:Min(1)
    val failureThreshold: Int? = null
)

data class ComputeAutoscalingGroupResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val name: String,
    val imageId: UUID,
    val flavor: String,
    val minInstances: Int,
    val maxInstances: Int,
    val desiredInstances: Int,
    val currentInstances: Int,
    val cpuScaleOutPercent: Int,
    val cpuScaleInPercent: Int,
    val memoryScaleOutPercent: Int,
    val memoryScaleInPercent: Int,
    val averageCpuPercent: Int?,
    val averageMemoryPercent: Int?,
    val healthPolicy: ComputeHealthPolicy,
    val failureThreshold: Int,
    val status: ComputeAutoscalingGroupStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(evaluation: ComputeAutoscalingEvaluation): ComputeAutoscalingGroupResponse {
            val group = evaluation.group
            return ComputeAutoscalingGroupResponse(
                id = group.id ?: error("autoscaling group id is null"),
                tenantId = group.tenantId,
                projectId = group.projectId,
                name = group.name,
                imageId = group.imageId,
                flavor = group.flavor,
                minInstances = group.minInstances,
                maxInstances = group.maxInstances,
                desiredInstances = group.desiredInstances,
                currentInstances = evaluation.currentInstances,
                cpuScaleOutPercent = group.cpuScaleOutPercent,
                cpuScaleInPercent = group.cpuScaleInPercent,
                memoryScaleOutPercent = group.memoryScaleOutPercent,
                memoryScaleInPercent = group.memoryScaleInPercent,
                averageCpuPercent = evaluation.averageCpuPercent,
                averageMemoryPercent = evaluation.averageMemoryPercent,
                healthPolicy = group.healthPolicy,
                failureThreshold = group.failureThreshold,
                status = group.status,
                createdAt = group.createdAt,
                updatedAt = group.updatedAt
            )
        }

        fun from(entity: ComputeAutoscalingGroupEntity, currentInstances: Int): ComputeAutoscalingGroupResponse {
            return ComputeAutoscalingGroupResponse(
                id = entity.id ?: error("autoscaling group id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                name = entity.name,
                imageId = entity.imageId,
                flavor = entity.flavor,
                minInstances = entity.minInstances,
                maxInstances = entity.maxInstances,
                desiredInstances = entity.desiredInstances,
                currentInstances = currentInstances,
                cpuScaleOutPercent = entity.cpuScaleOutPercent,
                cpuScaleInPercent = entity.cpuScaleInPercent,
                memoryScaleOutPercent = entity.memoryScaleOutPercent,
                memoryScaleInPercent = entity.memoryScaleInPercent,
                averageCpuPercent = null,
                averageMemoryPercent = null,
                healthPolicy = entity.healthPolicy,
                failureThreshold = entity.failureThreshold,
                status = entity.status,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class WriteComputeInstanceMetricRequest(
    @field:Min(0)
    @field:Max(100)
    val cpuPercent: Int,
    @field:Min(0)
    @field:Max(100)
    val memoryPercent: Int
)

data class ComputeInstanceMetricResponse(
    val instanceId: UUID,
    val cpuPercent: Int,
    val memoryPercent: Int,
    val reportedAt: LocalDateTime
) {
    companion object {
        fun from(entity: ComputeInstanceMetricEntity): ComputeInstanceMetricResponse {
            return ComputeInstanceMetricResponse(
                instanceId = entity.instanceId,
                cpuPercent = entity.cpuPercent,
                memoryPercent = entity.memoryPercent,
                reportedAt = entity.reportedAt
            )
        }
    }
}

data class WriteComputeInstanceHealthRequest(
    val status: ComputeHealthStatus,
    @field:Size(max = 500)
    val detail: String? = null
)

data class ComputeInstanceHealthResponse(
    val instanceId: UUID,
    val status: ComputeHealthStatus,
    val failureCount: Int,
    val detail: String?,
    val checkedAt: LocalDateTime
) {
    companion object {
        fun from(entity: ComputeInstanceHealthCheckEntity): ComputeInstanceHealthResponse {
            return ComputeInstanceHealthResponse(
                instanceId = entity.instanceId,
                status = entity.status,
                failureCount = entity.failureCount,
                detail = entity.detail,
                checkedAt = entity.checkedAt
            )
        }
    }
}

data class ComputeHealthReconcileResponse(
    val restartedInstanceIds: List<UUID>,
    val replacementInstanceIds: List<UUID>
) {
    companion object {
        fun from(result: ComputeHealthReconcileResult): ComputeHealthReconcileResponse {
            return ComputeHealthReconcileResponse(
                restartedInstanceIds = result.restartedInstanceIds,
                replacementInstanceIds = result.replacementInstanceIds
            )
        }
    }
}
