package com.stratuscloud.api.iam.controller

import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.ApiAuditRecorder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.api.iam.dto.CreateSecretRequest
import com.stratuscloud.api.iam.dto.RotateSecretRequest
import com.stratuscloud.api.iam.dto.SecretResponse
import com.stratuscloud.api.iam.dto.SecretVersionResponse
import com.stratuscloud.iam.service.IamAction
import com.stratuscloud.iam.service.SecretService
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
@RequestMapping("/v1/iam/secrets")
class SecretController(
    private val secretService: SecretService,
    private val authorizationFacade: AuthorizationFacade,
    private val apiAuditRecorder: ApiAuditRecorder
) {

    @PostMapping
    fun createSecret(
        @Valid @RequestBody request: CreateSecretRequest
    ): ResponseEntity<SecretResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.SECRET_CREATE,
            resource = request.projectId?.let { "project:$it" } ?: "tenant:${request.tenantId}",
            resourceType = "SECRET",
            resourceId = null,
            metadata = mapOf("name" to request.name)
        )
        val created = secretService.createSecret(
            tenantId = request.tenantId,
            projectId = request.projectId,
            name = request.name,
            value = request.value,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = created.secret.tenantId,
            projectId = created.secret.projectId,
            action = IamAction.SECRET_CREATE,
            resourceType = "SECRET",
            resourceId = created.secret.id.toString(),
            metadata = mapOf("name" to created.secret.name, "version" to created.currentVersion.version)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            SecretResponse.from(
                created.secret,
                currentVersion = SecretVersionResponse.from(created.currentVersion, rawValue = request.value)
            )
        )
    }

    @GetMapping
    fun listSecrets(
        @RequestParam tenantId: UUID,
        @RequestParam(required = false) projectId: UUID?
    ): ResponseEntity<List<SecretResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.SECRET_LIST,
            resource = projectId?.let { "project:$it" } ?: "tenant:$tenantId",
            resourceType = "SECRET",
            resourceId = tenantId.toString()
        )
        val response = secretService.listSecrets(tenantId, projectId).map { SecretResponse.from(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{secretId}")
    fun getSecret(
        @PathVariable secretId: UUID
    ): ResponseEntity<SecretResponse> {
        val principal = AuthContextHolder.getRequired()
        val secret = secretService.getSecret(secretId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = secret.tenantId,
            projectId = secret.projectId,
            action = IamAction.SECRET_READ,
            resource = "secret:$secretId",
            resourceType = "SECRET",
            resourceId = secretId.toString()
        )
        return ResponseEntity.ok(SecretResponse.from(secret))
    }

    @GetMapping("/{secretId}/versions")
    fun listSecretVersions(
        @PathVariable secretId: UUID
    ): ResponseEntity<List<SecretVersionResponse>> {
        val principal = AuthContextHolder.getRequired()
        val secret = secretService.getSecret(secretId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = secret.tenantId,
            projectId = secret.projectId,
            action = IamAction.SECRET_READ,
            resource = "secret:$secretId",
            resourceType = "SECRET",
            resourceId = secretId.toString()
        )
        val versions = secretService.listVersions(secretId).map { SecretVersionResponse.from(it) }
        return ResponseEntity.ok(versions)
    }

    @PostMapping("/{secretId}:rotate")
    fun rotateSecret(
        @PathVariable secretId: UUID,
        @Valid @RequestBody request: RotateSecretRequest
    ): ResponseEntity<SecretResponse> {
        val principal = AuthContextHolder.getRequired()
        val secret = secretService.getSecret(secretId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = secret.tenantId,
            projectId = secret.projectId,
            action = IamAction.SECRET_ROTATE,
            resource = "secret:$secretId",
            resourceType = "SECRET",
            resourceId = secretId.toString()
        )
        val rotated = secretService.rotateSecret(secretId, request.value, principal.actorId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = rotated.secret.tenantId,
            projectId = rotated.secret.projectId,
            action = IamAction.SECRET_ROTATE,
            resourceType = "SECRET",
            resourceId = rotated.secret.id.toString(),
            metadata = mapOf("version" to rotated.currentVersion.version)
        )
        return ResponseEntity.ok(
            SecretResponse.from(
                rotated.secret,
                currentVersion = SecretVersionResponse.from(rotated.currentVersion, rawValue = request.value)
            )
        )
    }

    @DeleteMapping("/{secretId}/versions/{versionId}")
    fun revokeSecretVersion(
        @PathVariable secretId: UUID,
        @PathVariable versionId: UUID
    ): ResponseEntity<SecretVersionResponse> {
        val principal = AuthContextHolder.getRequired()
        val secret = secretService.getSecret(secretId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = secret.tenantId,
            projectId = secret.projectId,
            action = IamAction.SECRET_VERSION_REVOKE,
            resource = "secret:$secretId",
            resourceType = "SECRET_VERSION",
            resourceId = versionId.toString()
        )
        val revoked = secretService.revokeVersion(secretId, versionId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = secret.tenantId,
            projectId = secret.projectId,
            action = IamAction.SECRET_VERSION_REVOKE,
            resourceType = "SECRET_VERSION",
            resourceId = revoked.id.toString(),
            metadata = mapOf("version" to revoked.version)
        )
        return ResponseEntity.ok(SecretVersionResponse.from(revoked))
    }
}
