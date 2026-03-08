package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkSecurityGroupEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkSecurityGroupRepository : JpaRepository<NetworkSecurityGroupEntity, UUID> {
    fun existsByVpcIdAndName(vpcId: UUID, name: String): Boolean
    fun existsByVpcId(vpcId: UUID): Boolean
    fun findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId: UUID, projectId: UUID): List<NetworkSecurityGroupEntity>
}
