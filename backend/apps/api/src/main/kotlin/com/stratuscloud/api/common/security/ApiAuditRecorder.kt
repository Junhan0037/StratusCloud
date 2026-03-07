package com.stratuscloud.api.common.security

import com.stratuscloud.audit.service.AuditEventService
import com.stratuscloud.iam.auth.AuthPrincipal
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ApiAuditRecorder(
    private val auditEventService: AuditEventService
) {

    fun recordSuccess(
        principal: AuthPrincipal,
        tenantId: UUID,
        projectId: UUID?,
        action: String,
        resourceType: String,
        resourceId: String?,
        metadata: Map<String, Any?> = emptyMap()
    ) {
        auditEventService.recordSuccess(
            actorId = principal.actorId,
            tenantId = tenantId,
            projectId = projectId,
            action = action,
            resourceType = resourceType,
            resourceId = resourceId,
            metadata = metadata
        )
    }
}
