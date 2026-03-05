package com.stratuscloud.api.iam.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.iam.dto.CreatePolicyRequest
import com.stratuscloud.api.iam.dto.PolicyResponse
import com.stratuscloud.iam.service.AuthorizationService
import com.stratuscloud.iam.service.IamAction
import com.stratuscloud.iam.service.PolicyService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/iam/policies")
class PolicyController(
    private val policyService: PolicyService,
    private val authorizationService: AuthorizationService,
    private val objectMapper: ObjectMapper
) {

    @PostMapping
    fun createPolicy(
        @Valid @RequestBody request: CreatePolicyRequest
    ): ResponseEntity<PolicyResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationService.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = null,
            action = IamAction.POLICY_CREATE,
            resource = "tenant:${request.tenantId}"
        )

        val created = policyService.createPolicy(
            tenantId = request.tenantId,
            name = request.name,
            description = request.description,
            document = objectMapper.writeValueAsString(request.document),
            actorId = principal.actorId
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(PolicyResponse.from(created, objectMapper))
    }

    @GetMapping
    fun listPolicies(
        @RequestParam tenantId: UUID
    ): ResponseEntity<List<PolicyResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationService.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = null,
            action = IamAction.POLICY_LIST,
            resource = "tenant:$tenantId"
        )
        val response = policyService.listPolicies(tenantId).map { PolicyResponse.from(it, objectMapper) }
        return ResponseEntity.ok(response)
    }
}
