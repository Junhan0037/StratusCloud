package com.stratuscloud.iam.service

import com.stratuscloud.iam.auth.AuthPrincipal
import com.stratuscloud.iam.domain.RoleType
import com.stratuscloud.iam.exception.ForbiddenException
import com.stratuscloud.iam.repository.PolicyRepository
import com.stratuscloud.iam.repository.RolePolicyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuthorizationService(
    private val rolePolicyRepository: RolePolicyRepository,
    private val policyRepository: PolicyRepository,
    private val policyEvaluator: PolicyEvaluator
) {

    @Transactional(readOnly = true)
    fun authorize(
        principal: AuthPrincipal,
        tenantId: UUID,
        projectId: UUID?,
        action: String,
        resource: String
    ) {
        if (principal.tenantId != null && principal.tenantId != tenantId) {
            throw ForbiddenException("tenant scope mismatch")
        }

        val roles = principal.resolveRoles(projectId)
        if (roles.isEmpty()) {
            throw ForbiddenException("no role bound to request scope")
        }

        val staticAllowed = roles
            .flatMap { DEFAULT_ACTIONS[it].orEmpty() }
            .toSet()
        if (action in staticAllowed || "*" in staticAllowed) {
            return
        }

        val rolePolicies = rolePolicyRepository.findAllByTenantIdAndRoleIn(tenantId, roles)
        if (rolePolicies.isEmpty()) {
            throw ForbiddenException("action is not allowed by policy")
        }
        val policyIds = rolePolicies.map { it.policyId }
        val policies = policyRepository.findAllByTenantIdAndIdIn(tenantId, policyIds)
        val allowed = policies.any { policy ->
            policyEvaluator.isActionAllowed(policy.document, action, resource)
        }
        if (!allowed) {
            throw ForbiddenException("action is not allowed by policy")
        }
    }

    companion object {
        // 기본 역할 권한은 Week 2 동작을 깨지 않도록 최소 정적 매핑으로 유지한다.
        private val DEFAULT_ACTIONS = mapOf(
            RoleType.OWNER to setOf("*"),
            RoleType.ADMIN to setOf(
                IamAction.PROJECT_CREATE,
                IamAction.PROJECT_READ,
                IamAction.USER_CREATE,
                IamAction.MEMBERSHIP_ADD,
                IamAction.MEMBERSHIP_ROLE_UPDATE,
                IamAction.POLICY_CREATE,
                IamAction.POLICY_LIST,
                IamAction.ROLE_POLICY_BIND,
                IamAction.API_KEY_CREATE,
                IamAction.API_KEY_REVOKE,
                IamAction.API_KEY_LIST,
                IamAction.SECRET_CREATE,
                IamAction.SECRET_LIST,
                IamAction.SECRET_READ,
                IamAction.SECRET_ROTATE,
                IamAction.SECRET_VERSION_REVOKE,
                IamAction.AUDIT_LOG_LIST
            ),
            RoleType.DEVELOPER to setOf(
                IamAction.PROJECT_READ,
                IamAction.POLICY_LIST,
                IamAction.API_KEY_LIST
            ),
            RoleType.VIEWER to setOf(
                IamAction.PROJECT_READ,
                IamAction.POLICY_LIST
            )
        )
    }
}
