package com.stratuscloud.governance.repository

import com.stratuscloud.governance.domain.StorageBucketMeteringEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StorageBucketMeteringRepository : JpaRepository<StorageBucketMeteringEntity, UUID> {
    fun findByBucketId(bucketId: UUID): StorageBucketMeteringEntity?
}
