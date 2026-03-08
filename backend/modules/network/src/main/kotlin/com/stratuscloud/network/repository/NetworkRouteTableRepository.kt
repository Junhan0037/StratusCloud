package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkRouteTableEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkRouteTableRepository : JpaRepository<NetworkRouteTableEntity, UUID> {
    fun existsByVpcIdAndName(vpcId: UUID, name: String): Boolean
    fun findAllByVpcIdOrderByCreatedAtDesc(vpcId: UUID): List<NetworkRouteTableEntity>
    fun countByVpcId(vpcId: UUID): Long
}
