package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkSubnetEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkSubnetRepository : JpaRepository<NetworkSubnetEntity, UUID> {
    fun existsByVpcIdAndName(vpcId: UUID, name: String): Boolean
    fun existsByVpcId(vpcId: UUID): Boolean
    fun findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId: UUID, projectId: UUID): List<NetworkSubnetEntity>
    fun findAllByVpcIdOrderByCreatedAtDesc(vpcId: UUID): List<NetworkSubnetEntity>
}
