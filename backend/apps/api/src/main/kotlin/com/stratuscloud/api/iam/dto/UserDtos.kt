package com.stratuscloud.api.iam.dto

import com.stratuscloud.iam.domain.UserEntity
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateUserRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val displayName: String
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val displayName: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: UserEntity): UserResponse {
            return UserResponse(
                id = user.id ?: error("user id is null"),
                email = user.email,
                displayName = user.displayName,
                createdAt = user.createdAt
            )
        }
    }
}
