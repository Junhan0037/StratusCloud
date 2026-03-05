package com.stratuscloud.api.iam.dto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.stratuscloud.iam.domain.ApiKeyEntity
import com.stratuscloud.iam.domain.ApiKeyStatus
import com.stratuscloud.iam.domain.PolicyEntity
import com.stratuscloud.iam.domain.RolePolicyEntity
import com.stratuscloud.iam.domain.RoleType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreatePolicyRequest(
    val tenantId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    @field:Size(max = 300)
    val description: String? = null,
    val document: JsonNode
)

data class PolicyResponse(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val description: String?,
    val document: JsonNode,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: PolicyEntity, objectMapper: ObjectMapper): PolicyResponse {
            return PolicyResponse(
                id = entity.id ?: error("policy id is null"),
                tenantId = entity.tenantId,
                name = entity.name,
                description = entity.description,
                document = objectMapper.readTree(entity.document),
                createdAt = entity.createdAt
            )
        }
    }
}

data class BindRolePoliciesRequest(
    val tenantId: UUID,
    val role: RoleType,
    @field:NotEmpty
    val policyIds: List<UUID>
)

data class RolePolicyResponse(
    val id: UUID,
    val tenantId: UUID,
    val role: RoleType,
    val policyId: UUID,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: RolePolicyEntity): RolePolicyResponse {
            return RolePolicyResponse(
                id = entity.id ?: error("role policy id is null"),
                tenantId = entity.tenantId,
                role = entity.role,
                policyId = entity.policyId,
                createdAt = entity.createdAt
            )
        }
    }
}

data class CreateApiKeyRequest(
    val tenantId: UUID,
    val projectId: UUID? = null,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    val role: RoleType,
    val expiresAt: LocalDateTime? = null
)

data class ApiKeyResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID?,
    val name: String,
    val role: RoleType,
    val keyPrefix: String,
    val status: ApiKeyStatus,
    val expiresAt: LocalDateTime,
    val revokedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val rawKey: String? = null
) {
    companion object {
        fun from(entity: ApiKeyEntity, rawKey: String? = null): ApiKeyResponse {
            return ApiKeyResponse(
                id = entity.id ?: error("api key id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                name = entity.name,
                role = entity.role,
                keyPrefix = entity.keyPrefix,
                status = entity.status,
                expiresAt = entity.expiresAt,
                revokedAt = entity.revokedAt,
                createdAt = entity.createdAt,
                rawKey = rawKey
            )
        }
    }
}
