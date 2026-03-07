package com.stratuscloud.api.common.security

import com.stratuscloud.audit.service.AuditEventService
import com.stratuscloud.iam.auth.AuthPrincipal
import com.stratuscloud.iam.exception.ForbiddenException
import com.stratuscloud.iam.service.AuthorizationService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AuthorizationFacade(
    private val authorizationService: AuthorizationService,
    private val auditEventService: AuditEventService
) {

    fun authorize(
        principal: AuthPrincipal,
        tenantId: UUID,
        projectId: UUID?,
        action: String,
        resource: String,
        resourceType: String,
        resourceId: String?,
        metadata: Map<String, Any?> = emptyMap()
    ) {
        try {
            authorizationService.authorize(
                principal = principal,
                tenantId = tenantId,
                projectId = projectId,
                action = action,
                resource = resource
            )
        } catch (ex: ForbiddenException) {
            auditEventService.recordDenied(
                actorId = principal.actorId,
                tenantId = tenantId,
                projectId = projectId,
                action = action,
                resourceType = resourceType,
                resourceId = resourceId,
                metadata = metadata + mapOf(
                    "resource" to resource,
                    "reason" to (ex.message ?: "forbidden")
                )
            )
            throw ex
        }
    }
}
