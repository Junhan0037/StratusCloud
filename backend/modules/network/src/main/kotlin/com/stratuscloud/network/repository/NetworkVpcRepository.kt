package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkVpcEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkVpcRepository : JpaRepository<NetworkVpcEntity, UUID> {
    fun existsByProjectIdAndName(projectId: UUID, name: String): Boolean
    fun findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId: UUID, projectId: UUID): List<NetworkVpcEntity>
}
