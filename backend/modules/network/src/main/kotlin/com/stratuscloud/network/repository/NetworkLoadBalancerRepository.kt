package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkLoadBalancerEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkLoadBalancerRepository : JpaRepository<NetworkLoadBalancerEntity, UUID> {
    fun existsByVpcIdAndName(vpcId: UUID, name: String): Boolean
    fun findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId: UUID, projectId: UUID): List<NetworkLoadBalancerEntity>
}
