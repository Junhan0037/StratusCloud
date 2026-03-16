package com.stratuscloud.api.data.dto

import com.stratuscloud.data.domain.ManagedDatabaseEngine
import com.stratuscloud.data.domain.ManagedDatabaseEntity
import com.stratuscloud.data.domain.ManagedDatabaseStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateManagedDatabaseRequest(
    val tenantId: UUID,
    val projectId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    @field:NotBlank
    @field:Size(min = 2, max = 30)
    val engineVersion: String,
    @field:NotBlank
    @field:Size(min = 2, max = 40)
    val instanceClass: String,
    @field:Min(10)
    @field:Max(1024)
    val storageGb: Int
)

data class ManagedDatabaseResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val name: String,
    val engine: ManagedDatabaseEngine,
    val engineVersion: String,
    val instanceClass: String,
    val storageGb: Int,
    val status: ManagedDatabaseStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: ManagedDatabaseEntity): ManagedDatabaseResponse {
            return ManagedDatabaseResponse(
                id = entity.id ?: error("managed database id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                name = entity.name,
                engine = entity.engine,
                engineVersion = entity.engineVersion,
                instanceClass = entity.instanceClass,
                storageGb = entity.storageGb,
                status = entity.status,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
