package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkSecurityGroupRuleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkSecurityGroupRuleRepository : JpaRepository<NetworkSecurityGroupRuleEntity, UUID> {
    fun findAllBySecurityGroupIdOrderByCreatedAtAsc(securityGroupId: UUID): List<NetworkSecurityGroupRuleEntity>
    fun deleteAllBySecurityGroupId(securityGroupId: UUID)
}
