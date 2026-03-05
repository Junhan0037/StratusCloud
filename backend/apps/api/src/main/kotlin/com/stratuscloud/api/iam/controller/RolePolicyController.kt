package com.stratuscloud.api.iam.controller

import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.iam.dto.BindRolePoliciesRequest
import com.stratuscloud.api.iam.dto.RolePolicyResponse
import com.stratuscloud.iam.service.AuthorizationService
import com.stratuscloud.iam.service.IamAction
import com.stratuscloud.iam.service.RolePolicyService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/iam/roles")
class RolePolicyController(
    private val rolePolicyService: RolePolicyService,
    private val authorizationService: AuthorizationService
) {

    @PostMapping
    fun bindRolePolicies(
        @Valid @RequestBody request: BindRolePoliciesRequest
    ): ResponseEntity<List<RolePolicyResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationService.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = null,
            action = IamAction.ROLE_POLICY_BIND,
            resource = "tenant:${request.tenantId}"
        )
        val bindings = rolePolicyService.replaceBindings(
            tenantId = request.tenantId,
            role = request.role,
            policyIds = request.policyIds,
            actorId = principal.actorId
        )
        return ResponseEntity.ok(bindings.map { RolePolicyResponse.from(it) })
    }
}
