package com.stratuscloud.api.system

import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.iam.domain.RoleType
import com.stratuscloud.iam.exception.ForbiddenException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/system/operations")
class SystemOperationsController(
    private val operationsMetricsService: OperationsMetricsService
) {

    @GetMapping("/summary")
    fun summary(): ResponseEntity<OperationsSummaryResponse> {
        val principal = AuthContextHolder.getRequired()
        requireOperationsAccess(principal.globalRoles + principal.projectRoles.values.flatten())
        return ResponseEntity.ok(operationsMetricsService.summary(principal.tenantId))
    }

    @GetMapping("/http-metrics")
    fun httpMetrics(): ResponseEntity<HttpMetricsResponse> {
        val principal = AuthContextHolder.getRequired()
        requireOperationsAccess(principal.globalRoles + principal.projectRoles.values.flatten())
        return ResponseEntity.ok(operationsMetricsService.httpMetrics())
    }

    private fun requireOperationsAccess(roles: Collection<RoleType>) {
        val allowed = roles.any { it == RoleType.OWNER || it == RoleType.ADMIN }
        if (!allowed) {
            throw ForbiddenException("operations access requires owner or admin role")
        }
    }
}
