package com.stratuscloud.storage.repository

import com.stratuscloud.storage.domain.StorageBucketEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StorageBucketRepository : JpaRepository<StorageBucketEntity, UUID> {
    fun existsByProjectIdAndName(projectId: UUID, name: String): Boolean
    fun findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId: UUID, projectId: UUID): List<StorageBucketEntity>
    fun countByProjectId(projectId: UUID): Long
}
