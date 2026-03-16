package com.stratuscloud.data.repository

import com.stratuscloud.data.domain.ManagedDatabaseEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ManagedDatabaseRepository : JpaRepository<ManagedDatabaseEntity, UUID> {
    fun existsByProjectIdAndName(projectId: UUID, name: String): Boolean
    fun findAllByTenantIdAndProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        tenantId: UUID,
        projectId: UUID
    ): List<ManagedDatabaseEntity>
}
