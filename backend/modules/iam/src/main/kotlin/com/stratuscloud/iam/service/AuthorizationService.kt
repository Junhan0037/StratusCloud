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
                IamAction.AUDIT_LOG_LIST,
                IamAction.COMPUTE_IMAGE_CREATE,
                IamAction.COMPUTE_IMAGE_LIST,
                IamAction.COMPUTE_IMAGE_READ,
                IamAction.COMPUTE_INSTANCE_CREATE,
                IamAction.COMPUTE_INSTANCE_LIST,
                IamAction.COMPUTE_INSTANCE_READ,
                IamAction.COMPUTE_INSTANCE_START,
                IamAction.COMPUTE_INSTANCE_STOP,
                IamAction.COMPUTE_INSTANCE_TERMINATE,
                IamAction.COMPUTE_AUTOSCALING_GROUP_CREATE,
                IamAction.COMPUTE_AUTOSCALING_GROUP_LIST,
                IamAction.COMPUTE_AUTOSCALING_GROUP_READ,
                IamAction.COMPUTE_AUTOSCALING_GROUP_EVALUATE,
                IamAction.COMPUTE_AUTOSCALING_GROUP_RECONCILE,
                IamAction.COMPUTE_INSTANCE_METRIC_WRITE,
                IamAction.COMPUTE_INSTANCE_HEALTH_WRITE,
                IamAction.NETWORK_VPC_CREATE,
                IamAction.NETWORK_VPC_LIST,
                IamAction.NETWORK_VPC_READ,
                IamAction.NETWORK_VPC_DELETE,
                IamAction.NETWORK_SUBNET_CREATE,
                IamAction.NETWORK_SUBNET_LIST,
                IamAction.NETWORK_SUBNET_READ,
                IamAction.NETWORK_SUBNET_DELETE,
                IamAction.NETWORK_ROUTE_TABLE_CREATE,
                IamAction.NETWORK_ROUTE_TABLE_LIST,
                IamAction.NETWORK_ROUTE_TABLE_READ,
                IamAction.NETWORK_ROUTE_TABLE_DELETE,
                IamAction.NETWORK_ROUTE_CREATE,
                IamAction.NETWORK_ROUTE_DELETE,
                IamAction.NETWORK_ROUTE_TABLE_ASSOCIATE,
                IamAction.NETWORK_ROUTE_TABLE_DISASSOCIATE,
                IamAction.NETWORK_SECURITY_GROUP_CREATE,
                IamAction.NETWORK_SECURITY_GROUP_LIST,
                IamAction.NETWORK_SECURITY_GROUP_READ,
                IamAction.NETWORK_SECURITY_GROUP_DELETE,
                IamAction.NETWORK_SECURITY_GROUP_RULES_WRITE,
                IamAction.NETWORK_LOAD_BALANCER_CREATE,
                IamAction.NETWORK_LOAD_BALANCER_LIST,
                IamAction.NETWORK_LOAD_BALANCER_READ,
                IamAction.NETWORK_LOAD_BALANCER_DELETE,
                IamAction.NETWORK_LOAD_BALANCER_LISTENER_CREATE,
                IamAction.NETWORK_LOAD_BALANCER_RULE_CREATE,
                IamAction.NETWORK_ELASTIC_IP_CREATE,
                IamAction.NETWORK_ELASTIC_IP_LIST,
                IamAction.NETWORK_ELASTIC_IP_READ,
                IamAction.NETWORK_ELASTIC_IP_DELETE,
                IamAction.NETWORK_ELASTIC_IP_ATTACH,
                IamAction.NETWORK_ELASTIC_IP_DETACH,
                IamAction.NETWORK_NAT_GATEWAY_CREATE,
                IamAction.NETWORK_NAT_GATEWAY_LIST,
                IamAction.NETWORK_NAT_GATEWAY_READ,
                IamAction.NETWORK_NAT_GATEWAY_DELETE,
                IamAction.NETWORK_DNS_RECORD_CREATE,
                IamAction.NETWORK_DNS_RECORD_LIST,
                IamAction.NETWORK_DNS_RECORD_READ,
                IamAction.NETWORK_DNS_RECORD_DELETE
            ),
            RoleType.DEVELOPER to setOf(
                IamAction.PROJECT_READ,
                IamAction.POLICY_LIST,
                IamAction.API_KEY_LIST,
                IamAction.COMPUTE_IMAGE_LIST,
                IamAction.COMPUTE_IMAGE_READ,
                IamAction.COMPUTE_INSTANCE_CREATE,
                IamAction.COMPUTE_INSTANCE_LIST,
                IamAction.COMPUTE_INSTANCE_READ,
                IamAction.COMPUTE_INSTANCE_START,
                IamAction.COMPUTE_INSTANCE_STOP,
                IamAction.COMPUTE_INSTANCE_TERMINATE,
                IamAction.COMPUTE_AUTOSCALING_GROUP_CREATE,
                IamAction.COMPUTE_AUTOSCALING_GROUP_LIST,
                IamAction.COMPUTE_AUTOSCALING_GROUP_READ,
                IamAction.COMPUTE_AUTOSCALING_GROUP_EVALUATE,
                IamAction.COMPUTE_AUTOSCALING_GROUP_RECONCILE,
                IamAction.COMPUTE_INSTANCE_METRIC_WRITE,
                IamAction.COMPUTE_INSTANCE_HEALTH_WRITE,
                IamAction.NETWORK_VPC_CREATE,
                IamAction.NETWORK_VPC_LIST,
                IamAction.NETWORK_VPC_READ,
                IamAction.NETWORK_VPC_DELETE,
                IamAction.NETWORK_SUBNET_CREATE,
                IamAction.NETWORK_SUBNET_LIST,
                IamAction.NETWORK_SUBNET_READ,
                IamAction.NETWORK_SUBNET_DELETE,
                IamAction.NETWORK_ROUTE_TABLE_CREATE,
                IamAction.NETWORK_ROUTE_TABLE_LIST,
                IamAction.NETWORK_ROUTE_TABLE_READ,
                IamAction.NETWORK_ROUTE_TABLE_DELETE,
                IamAction.NETWORK_ROUTE_CREATE,
                IamAction.NETWORK_ROUTE_DELETE,
                IamAction.NETWORK_ROUTE_TABLE_ASSOCIATE,
                IamAction.NETWORK_ROUTE_TABLE_DISASSOCIATE,
                IamAction.NETWORK_SECURITY_GROUP_CREATE,
                IamAction.NETWORK_SECURITY_GROUP_LIST,
                IamAction.NETWORK_SECURITY_GROUP_READ,
                IamAction.NETWORK_SECURITY_GROUP_DELETE,
                IamAction.NETWORK_SECURITY_GROUP_RULES_WRITE,
                IamAction.NETWORK_LOAD_BALANCER_CREATE,
                IamAction.NETWORK_LOAD_BALANCER_LIST,
                IamAction.NETWORK_LOAD_BALANCER_READ,
                IamAction.NETWORK_LOAD_BALANCER_DELETE,
                IamAction.NETWORK_LOAD_BALANCER_LISTENER_CREATE,
                IamAction.NETWORK_LOAD_BALANCER_RULE_CREATE,
                IamAction.NETWORK_ELASTIC_IP_CREATE,
                IamAction.NETWORK_ELASTIC_IP_LIST,
                IamAction.NETWORK_ELASTIC_IP_READ,
                IamAction.NETWORK_ELASTIC_IP_DELETE,
                IamAction.NETWORK_ELASTIC_IP_ATTACH,
                IamAction.NETWORK_ELASTIC_IP_DETACH,
                IamAction.NETWORK_NAT_GATEWAY_CREATE,
                IamAction.NETWORK_NAT_GATEWAY_LIST,
                IamAction.NETWORK_NAT_GATEWAY_READ,
                IamAction.NETWORK_NAT_GATEWAY_DELETE,
                IamAction.NETWORK_DNS_RECORD_CREATE,
                IamAction.NETWORK_DNS_RECORD_LIST,
                IamAction.NETWORK_DNS_RECORD_READ,
                IamAction.NETWORK_DNS_RECORD_DELETE
            ),
            RoleType.VIEWER to setOf(
                IamAction.PROJECT_READ,
                IamAction.POLICY_LIST,
                IamAction.COMPUTE_IMAGE_LIST,
                IamAction.COMPUTE_IMAGE_READ,
                IamAction.COMPUTE_AUTOSCALING_GROUP_LIST,
                IamAction.COMPUTE_AUTOSCALING_GROUP_READ,
                IamAction.COMPUTE_INSTANCE_LIST,
                IamAction.COMPUTE_INSTANCE_READ,
                IamAction.NETWORK_VPC_LIST,
                IamAction.NETWORK_VPC_READ,
                IamAction.NETWORK_SUBNET_LIST,
                IamAction.NETWORK_SUBNET_READ,
                IamAction.NETWORK_ROUTE_TABLE_LIST,
                IamAction.NETWORK_ROUTE_TABLE_READ,
                IamAction.NETWORK_SECURITY_GROUP_LIST,
                IamAction.NETWORK_SECURITY_GROUP_READ,
                IamAction.NETWORK_LOAD_BALANCER_LIST,
                IamAction.NETWORK_LOAD_BALANCER_READ,
                IamAction.NETWORK_ELASTIC_IP_LIST,
                IamAction.NETWORK_ELASTIC_IP_READ,
                IamAction.NETWORK_NAT_GATEWAY_LIST,
                IamAction.NETWORK_NAT_GATEWAY_READ,
                IamAction.NETWORK_DNS_RECORD_LIST,
                IamAction.NETWORK_DNS_RECORD_READ
            )
        )
    }
}
