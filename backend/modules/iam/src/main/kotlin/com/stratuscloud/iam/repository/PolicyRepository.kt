package com.stratuscloud.iam.repository

import com.stratuscloud.iam.domain.PolicyEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PolicyRepository : JpaRepository<PolicyEntity, UUID> {
    fun existsByTenantIdAndName(tenantId: UUID, name: String): Boolean
    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<PolicyEntity>
    fun findAllByTenantIdAndIdIn(tenantId: UUID, ids: Collection<UUID>): List<PolicyEntity>
}
