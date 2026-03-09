package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkRouteEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkRouteRepository : JpaRepository<NetworkRouteEntity, UUID> {
    fun existsByRouteTableIdAndDestinationCidr(routeTableId: UUID, destinationCidr: String): Boolean
    fun findAllByRouteTableIdOrderByCreatedAtAsc(routeTableId: UUID): List<NetworkRouteEntity>
    fun existsByTargetTypeAndTargetResourceId(targetType: com.stratuscloud.network.domain.NetworkRouteTargetType, targetResourceId: UUID): Boolean
    fun deleteAllByRouteTableId(routeTableId: UUID)
}
