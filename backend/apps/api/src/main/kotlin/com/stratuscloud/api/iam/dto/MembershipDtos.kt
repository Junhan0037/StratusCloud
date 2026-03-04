package com.stratuscloud.api.iam.dto

import com.stratuscloud.iam.domain.ProjectMembershipEntity
import com.stratuscloud.iam.domain.RoleType
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.UUID

data class AddMembershipRequest(
    val userId: UUID,
    @field:NotNull
    val role: RoleType
)

data class UpdateMembershipRoleRequest(
    @field:NotNull
    val role: RoleType
)

data class MembershipResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val userId: UUID,
    val role: RoleType,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(membership: ProjectMembershipEntity): MembershipResponse {
            return MembershipResponse(
                id = membership.id ?: error("membership id is null"),
                tenantId = membership.tenantId,
                projectId = membership.projectId,
                userId = membership.userId,
                role = membership.role,
                createdAt = membership.createdAt
            )
        }
    }
}
