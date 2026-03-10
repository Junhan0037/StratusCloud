package com.stratuscloud.storage.repository

import com.stratuscloud.storage.domain.StoragePresignedTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StoragePresignedTokenRepository : JpaRepository<StoragePresignedTokenEntity, UUID>
{
    fun deleteAllByBucketId(bucketId: UUID)
}
