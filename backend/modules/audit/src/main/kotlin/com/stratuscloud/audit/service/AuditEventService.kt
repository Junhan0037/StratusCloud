package com.stratuscloud.audit.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.stratuscloud.audit.domain.AuditEventEntity
import com.stratuscloud.audit.domain.AuditResult
import com.stratuscloud.audit.repository.AuditEventRepository
import com.stratuscloud.common.trace.TraceIdProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

data class AuditEventFilter(
    val tenantId: UUID? = null,
    val projectId: UUID? = null,
    val actorId: UUID? = null,
    val resourceType: String? = null,
    val action: String? = null,
    val result: AuditResult? = null,
    val occurredFrom: LocalDateTime? = null,
    val occurredTo: LocalDateTime? = null
)

@Service
class AuditEventService(
    private val auditEventRepository: AuditEventRepository,
    private val objectMapper: ObjectMapper,
    private val traceIdProvider: TraceIdProvider
) {

    @Transactional
    fun recordSuccess(
        actorId: UUID,
        tenantId: UUID,
        projectId: UUID?,
        action: String,
        resourceType: String,
        resourceId: String?,
        metadata: Map<String, Any?> = emptyMap()
    ): AuditEventEntity {
        return save(
            actorId = actorId,
            tenantId = tenantId,
            projectId = projectId,
            action = action,
            resourceType = resourceType,
            resourceId = resourceId,
            result = AuditResult.SUCCESS,
            metadata = metadata
        )
    }

    @Transactional
    fun recordDenied(
        actorId: UUID,
        tenantId: UUID,
        projectId: UUID?,
        action: String,
        resourceType: String,
        resourceId: String?,
        metadata: Map<String, Any?> = emptyMap()
    ): AuditEventEntity {
        return save(
            actorId = actorId,
            tenantId = tenantId,
            projectId = projectId,
            action = action,
            resourceType = resourceType,
            resourceId = resourceId,
            result = AuditResult.DENIED,
            metadata = metadata
        )
    }

    @Transactional(readOnly = true)
    fun list(filter: AuditEventFilter): List<AuditEventEntity> {
        return auditEventRepository.search(
            tenantId = filter.tenantId,
            projectId = filter.projectId,
            actorId = filter.actorId,
            resourceType = filter.resourceType?.trim()?.uppercase(),
            action = filter.action?.trim(),
            result = filter.result,
            occurredFrom = filter.occurredFrom,
            occurredTo = filter.occurredTo
        )
    }

    private fun save(
        actorId: UUID,
        tenantId: UUID,
        projectId: UUID?,
        action: String,
        resourceType: String,
        resourceId: String?,
        result: AuditResult,
        metadata: Map<String, Any?>
    ): AuditEventEntity {
        return auditEventRepository.save(
            AuditEventEntity(
                traceId = traceIdProvider.newTraceId(),
                actorId = actorId,
                tenantId = tenantId,
                projectId = projectId,
                action = action,
                resourceType = resourceType.uppercase(),
                resourceId = resourceId,
                result = result,
                metadata = objectMapper.writeValueAsString(metadata),
                occurredAt = LocalDateTime.now()
            )
        )
    }
}
