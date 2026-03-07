package com.stratuscloud.api.audit.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.stratuscloud.api.audit.dto.AuditLogResponse
import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.audit.domain.AuditResult
import com.stratuscloud.audit.service.AuditEventFilter
import com.stratuscloud.audit.service.AuditEventService
import com.stratuscloud.iam.service.IamAction
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/v1/audit/logs")
class AuditLogController(
    private val authorizationFacade: AuthorizationFacade,
    private val auditEventService: AuditEventService,
    private val objectMapper: ObjectMapper
) {

    @GetMapping
    fun listAuditLogs(
        @RequestParam tenantId: UUID,
        @RequestParam(required = false) projectId: UUID?,
        @RequestParam(required = false) actorId: UUID?,
        @RequestParam(required = false) resourceType: String?,
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) result: AuditResult?,
        @RequestParam("from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        occurredFrom: LocalDateTime?,
        @RequestParam("to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        occurredTo: LocalDateTime?
    ): ResponseEntity<List<AuditLogResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.AUDIT_LOG_LIST,
            resource = projectId?.let { "project:$it" } ?: "tenant:$tenantId",
            resourceType = "AUDIT_LOG",
            resourceId = tenantId.toString()
        )
        val logs = auditEventService.list(
            AuditEventFilter(
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                resourceType = resourceType,
                action = action,
                result = result,
                occurredFrom = occurredFrom,
                occurredTo = occurredTo
            )
        )
        return ResponseEntity.ok(logs.map { AuditLogResponse.from(it, objectMapper) })
    }
}
