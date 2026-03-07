package com.stratuscloud.api.iam.controller

import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.ApiAuditRecorder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.api.iam.dto.CreateUserRequest
import com.stratuscloud.api.iam.dto.UserResponse
import com.stratuscloud.iam.service.IamAction
import com.stratuscloud.iam.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/iam/users")
class IamUserController(
    private val userService: UserService,
    private val authorizationFacade: AuthorizationFacade,
    private val apiAuditRecorder: ApiAuditRecorder
) {

    @PostMapping
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest
    ): ResponseEntity<UserResponse> {
        val principal = AuthContextHolder.getRequired()
        val tenantId = principal.tenantId ?: LEGACY_TENANT_ID
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = null,
            action = IamAction.USER_CREATE,
            resource = "tenant:$tenantId",
            resourceType = "USER",
            resourceId = null,
            metadata = mapOf("email" to request.email)
        )
        val created = userService.createUser(request.email, request.displayName, principal.actorId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = tenantId,
            projectId = null,
            action = IamAction.USER_CREATE,
            resourceType = "USER",
            resourceId = created.id.toString(),
            metadata = mapOf("email" to created.email)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created))
    }

    companion object {
        private val LEGACY_TENANT_ID: UUID = UUID(0, 0)
    }
}
