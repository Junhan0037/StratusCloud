package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkLoadBalancerRuleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkLoadBalancerRuleRepository : JpaRepository<NetworkLoadBalancerRuleEntity, UUID> {
    fun existsByListenerIdAndPriority(listenerId: UUID, priority: Int): Boolean
    fun findAllByListenerIdOrderByPriorityAsc(listenerId: UUID): List<NetworkLoadBalancerRuleEntity>
    fun deleteAllByListenerId(listenerId: UUID)
}
