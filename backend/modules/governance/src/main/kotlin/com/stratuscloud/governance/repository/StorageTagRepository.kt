package com.stratuscloud.governance.repository

import com.stratuscloud.governance.domain.StorageTagEntity
import com.stratuscloud.governance.domain.StorageTagResourceType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StorageTagRepository : JpaRepository<StorageTagEntity, UUID> {
    fun findAllByResourceTypeAndResourceIdOrderByCreatedAtAsc(
        resourceType: StorageTagResourceType,
        resourceId: UUID
    ): List<StorageTagEntity>

    fun deleteAllByResourceTypeAndResourceId(resourceType: StorageTagResourceType, resourceId: UUID)
}
