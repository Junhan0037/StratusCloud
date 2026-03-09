package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkDnsRecordEntity
import com.stratuscloud.network.domain.NetworkDnsTargetType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkDnsRecordRepository : JpaRepository<NetworkDnsRecordEntity, UUID> {
    fun existsByProjectIdAndName(projectId: UUID, name: String): Boolean
    fun findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId: UUID, projectId: UUID): List<NetworkDnsRecordEntity>
    fun existsByTargetTypeAndTargetId(targetType: NetworkDnsTargetType, targetId: UUID): Boolean
}
