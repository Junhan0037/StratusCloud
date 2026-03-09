package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkLoadBalancerListenerEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkLoadBalancerListenerRepository : JpaRepository<NetworkLoadBalancerListenerEntity, UUID> {
    fun existsByLoadBalancerIdAndPort(loadBalancerId: UUID, port: Int): Boolean
    fun findAllByLoadBalancerIdOrderByCreatedAtAsc(loadBalancerId: UUID): List<NetworkLoadBalancerListenerEntity>
    fun deleteAllByLoadBalancerId(loadBalancerId: UUID)
}
