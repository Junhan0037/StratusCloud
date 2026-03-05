package com.stratuscloud.iam.repository

import com.stratuscloud.iam.domain.RolePolicyEntity
import com.stratuscloud.iam.domain.RoleType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RolePolicyRepository : JpaRepository<RolePolicyEntity, UUID> {
    fun findAllByTenantIdAndRoleIn(tenantId: UUID, roles: Collection<RoleType>): List<RolePolicyEntity>
    fun deleteAllByTenantIdAndRole(tenantId: UUID, role: RoleType)
}
