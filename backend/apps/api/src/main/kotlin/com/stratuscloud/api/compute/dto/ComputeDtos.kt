package com.stratuscloud.api.compute.dto

import com.stratuscloud.compute.domain.ComputeImageEntity
import com.stratuscloud.compute.domain.ComputeImageStatus
import com.stratuscloud.compute.domain.ComputeInstanceEntity
import com.stratuscloud.compute.domain.ComputeInstanceStatus
import com.stratuscloud.compute.domain.ComputeOsType
import jakarta.validation.constraints.NotBlank
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
    val name: String,
    val flavor: String,
    val status: ComputeInstanceStatus,
    val userData: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val lastTransitionAt: LocalDateTime
) {
    companion object {
        fun from(entity: ComputeInstanceEntity): ComputeInstanceResponse {
            return ComputeInstanceResponse(
                id = entity.id ?: error("compute instance id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                imageId = entity.imageId,
                name = entity.name,
                flavor = entity.flavor,
                status = entity.status,
                userData = entity.userData,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                lastTransitionAt = entity.lastTransitionAt
            )
        }
    }
}
