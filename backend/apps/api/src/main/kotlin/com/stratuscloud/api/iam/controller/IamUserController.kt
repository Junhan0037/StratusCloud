package com.stratuscloud.api.iam.controller

import com.stratuscloud.api.iam.dto.CreateUserRequest
import com.stratuscloud.api.iam.dto.UserResponse
import com.stratuscloud.iam.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/iam/users")
class IamUserController(
    private val userService: UserService
) {

    @PostMapping
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest,
        @RequestHeader("X-Actor-Id") actorId: UUID
    ): ResponseEntity<UserResponse> {
        val created = userService.createUser(request.email, request.displayName, actorId)
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created))
    }
}
