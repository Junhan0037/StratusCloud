package com.stratuscloud.iam.service

import com.stratuscloud.iam.domain.RolePolicyEntity
import com.stratuscloud.iam.domain.RoleType
import com.stratuscloud.iam.exception.BadRequestException
import com.stratuscloud.iam.repository.PolicyRepository
import com.stratuscloud.iam.repository.RolePolicyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RolePolicyService(
    private val rolePolicyRepository: RolePolicyRepository,
    private val policyRepository: PolicyRepository
) {

    @Transactional
    fun replaceBindings(
        tenantId: UUID,
        role: RoleType,
        policyIds: List<UUID>,
        actorId: UUID
    ): List<RolePolicyEntity> {
        val distinctPolicyIds = policyIds.distinct()
        val policies = policyRepository.findAllByTenantIdAndIdIn(tenantId, distinctPolicyIds)
        if (policies.size != distinctPolicyIds.size) {
            throw BadRequestException("policy ids contain entries that are not in tenant")
        }

        rolePolicyRepository.deleteAllByTenantIdAndRole(tenantId, role)
        return distinctPolicyIds.map { policyId ->
            rolePolicyRepository.save(
                RolePolicyEntity(
                    tenantId = tenantId,
                    role = role,
                    policyId = policyId,
                    createdBy = actorId.toString()
                )
            )
        }
    }
}
