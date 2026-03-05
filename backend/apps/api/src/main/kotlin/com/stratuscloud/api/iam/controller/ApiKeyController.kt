package com.stratuscloud.api.iam.controller

import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.iam.dto.ApiKeyResponse
import com.stratuscloud.api.iam.dto.CreateApiKeyRequest
import com.stratuscloud.iam.service.ApiKeyService
import com.stratuscloud.iam.service.AuthorizationService
import com.stratuscloud.iam.service.IamAction
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/iam/api-keys")
class ApiKeyController(
    private val apiKeyService: ApiKeyService,
    private val authorizationService: AuthorizationService
) {

    @PostMapping
    fun createApiKey(
        @Valid @RequestBody request: CreateApiKeyRequest
    ): ResponseEntity<ApiKeyResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationService.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.API_KEY_CREATE,
            resource = request.projectId?.let { "project:$it" } ?: "tenant:${request.tenantId}"
        )
        val issued = apiKeyService.issueApiKey(
            tenantId = request.tenantId,
            projectId = request.projectId,
            name = request.name,
            role = request.role,
            requestedExpiresAt = request.expiresAt,
            actorId = principal.actorId
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiKeyResponse.from(issued.entity, rawKey = issued.rawKey))
    }

    @GetMapping
    fun listApiKeys(
        @RequestParam tenantId: UUID
    ): ResponseEntity<List<ApiKeyResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationService.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = null,
            action = IamAction.API_KEY_LIST,
            resource = "tenant:$tenantId"
        )
        val response = apiKeyService.listApiKeys(tenantId).map { ApiKeyResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{keyId}")
    fun revokeApiKey(
        @PathVariable keyId: UUID
    ): ResponseEntity<ApiKeyResponse> {
        val principal = AuthContextHolder.getRequired()
        val key = apiKeyService.getApiKey(keyId)
        authorizationService.authorize(
            principal = principal,
            tenantId = key.tenantId,
            projectId = key.projectId,
            action = IamAction.API_KEY_REVOKE,
            resource = key.projectId?.let { "project:$it" } ?: "tenant:${key.tenantId}"
        )
        val revoked = apiKeyService.revokeApiKey(keyId)
        return ResponseEntity.ok(ApiKeyResponse.from(revoked))
    }
}
