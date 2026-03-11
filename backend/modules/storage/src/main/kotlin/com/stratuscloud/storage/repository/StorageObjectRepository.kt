package com.stratuscloud.storage.repository

import com.stratuscloud.storage.domain.StorageObjectEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface StorageObjectRepository : JpaRepository<StorageObjectEntity, UUID> {
    fun countByBucketId(bucketId: UUID): Long
    fun countByProjectId(projectId: UUID): Long

    @Query("select coalesce(sum(o.sizeBytes), 0) from StorageObjectEntity o where o.projectId = :projectId")
    fun sumSizeBytesByProjectId(projectId: UUID): Long

    fun existsByBucketIdAndKey(bucketId: UUID, key: String): Boolean
    fun findAllByBucketIdOrderByCreatedAtDesc(bucketId: UUID): List<StorageObjectEntity>
    fun findByBucketIdAndKey(bucketId: UUID, key: String): StorageObjectEntity?
}
