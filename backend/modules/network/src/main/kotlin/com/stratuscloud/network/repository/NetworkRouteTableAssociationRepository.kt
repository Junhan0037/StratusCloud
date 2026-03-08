package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkRouteTableAssociationEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkRouteTableAssociationRepository : JpaRepository<NetworkRouteTableAssociationEntity, UUID> {
    fun existsByRouteTableId(routeTableId: UUID): Boolean
    fun findAllByRouteTableIdOrderByCreatedAtAsc(routeTableId: UUID): List<NetworkRouteTableAssociationEntity>
    fun findBySubnetId(subnetId: UUID): NetworkRouteTableAssociationEntity?
    fun deleteBySubnetId(subnetId: UUID)
}
