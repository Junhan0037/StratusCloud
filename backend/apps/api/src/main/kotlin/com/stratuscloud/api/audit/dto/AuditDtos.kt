package com.stratuscloud.api.audit.dto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.stratuscloud.audit.domain.AuditEventEntity
import com.stratuscloud.audit.domain.AuditResult
import java.time.LocalDateTime
import java.util.UUID

data class AuditLogResponse(
    val id: UUID,
    val traceId: String,
    val actorId: UUID,
    val tenantId: UUID,
    val projectId: UUID?,
    val action: String,
    val resourceType: String,
    val resourceId: String?,
    val result: AuditResult,
    val metadata: JsonNode,
    val occurredAt: LocalDateTime
) {
    companion object {
        fun from(entity: AuditEventEntity, objectMapper: ObjectMapper): AuditLogResponse {
            return AuditLogResponse(
                id = entity.id ?: error("audit event id is null"),
                traceId = entity.traceId,
                actorId = entity.actorId,
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                action = entity.action,
                resourceType = entity.resourceType,
                resourceId = entity.resourceId,
                result = entity.result,
                metadata = objectMapper.readTree(entity.metadata),
                occurredAt = entity.occurredAt
            )
        }
    }
}
