package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkNatGatewayEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkNatGatewayRepository : JpaRepository<NetworkNatGatewayEntity, UUID> {
    fun existsByVpcIdAndName(vpcId: UUID, name: String): Boolean
    fun existsBySubnetId(subnetId: UUID): Boolean
    fun findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId: UUID, projectId: UUID): List<NetworkNatGatewayEntity>
}
