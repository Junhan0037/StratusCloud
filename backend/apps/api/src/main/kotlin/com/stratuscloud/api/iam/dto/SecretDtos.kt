package com.stratuscloud.api.iam.dto

import com.stratuscloud.iam.domain.SecretEntity
import com.stratuscloud.iam.domain.SecretVersionEntity
import com.stratuscloud.iam.domain.SecretVersionStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateSecretRequest(
    val tenantId: UUID,
    val projectId: UUID? = null,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    @field:NotBlank
    @field:Size(min = 1, max = 4096)
    val value: String
)

data class RotateSecretRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 4096)
    val value: String
)

data class SecretVersionResponse(
    val id: UUID,
    val version: Int,
    val status: SecretVersionStatus,
    val revokedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val value: String? = null
) {
    companion object {
        fun from(entity: SecretVersionEntity, rawValue: String? = null): SecretVersionResponse {
            return SecretVersionResponse(
                id = entity.id ?: error("secret version id is null"),
                version = entity.version,
                status = entity.status,
                revokedAt = entity.revokedAt,
                createdAt = entity.createdAt,
                value = rawValue
            )
        }
    }
}

data class SecretResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID?,
    val name: String,
    val latestVersion: Int,
    val createdAt: LocalDateTime,
    val currentVersion: SecretVersionResponse? = null
) {
    companion object {
        fun from(entity: SecretEntity, currentVersion: SecretVersionResponse? = null): SecretResponse {
            return SecretResponse(
                id = entity.id ?: error("secret id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                name = entity.name,
                latestVersion = entity.latestVersion,
                createdAt = entity.createdAt,
                currentVersion = currentVersion
            )
        }
    }
}
