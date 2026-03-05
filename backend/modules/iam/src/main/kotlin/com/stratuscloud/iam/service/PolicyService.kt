package com.stratuscloud.iam.service

import com.stratuscloud.iam.domain.PolicyEntity
import com.stratuscloud.iam.exception.BadRequestException
import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.repository.PolicyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PolicyService(
    private val policyRepository: PolicyRepository,
    private val policyEvaluator: PolicyEvaluator
) {

    @Transactional
    fun createPolicy(
        tenantId: UUID,
        name: String,
        description: String?,
        document: String,
        actorId: UUID
    ): PolicyEntity {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            throw BadRequestException("policy name must not be blank")
        }
        if (policyRepository.existsByTenantIdAndName(tenantId, normalizedName)) {
            throw DuplicateResourceException("policy name already exists in tenant")
        }
        if (!policyEvaluator.validateDocument(document)) {
            throw BadRequestException("invalid policy document: statements must be non-empty array")
        }

        return policyRepository.save(
            PolicyEntity(
                tenantId = tenantId,
                name = normalizedName,
                description = description?.trim()?.ifBlank { null },
                document = document.trim(),
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional(readOnly = true)
    fun listPolicies(tenantId: UUID): List<PolicyEntity> {
        return policyRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
    }
}
