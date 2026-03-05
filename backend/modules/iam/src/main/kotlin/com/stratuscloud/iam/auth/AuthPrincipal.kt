package com.stratuscloud.iam.auth

import com.stratuscloud.iam.domain.RoleType
import java.util.UUID

data class AuthPrincipal(
    val actorId: UUID,
    val tenantId: UUID?,
    val globalRoles: Set<RoleType>,
    val projectRoles: Map<UUID, Set<RoleType>>,
    val authMethod: AuthMethod,
    val apiKeyId: UUID? = null
) {
    fun resolveRoles(projectId: UUID?): Set<RoleType> {
        // 프로젝트 문맥이 있으면 프로젝트 역할 + 전역 역할을 함께 평가한다.
        if (projectId != null) {
            return (projectRoles[projectId].orEmpty() + globalRoles).toSet()
        }
        return globalRoles
    }
}
