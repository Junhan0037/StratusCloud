package com.stratuscloud.governance.domain

import com.stratuscloud.iam.domain.BaseAuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "governance_storage_bucket_metering")
class StorageBucketMeteringEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(name = "bucket_id", nullable = false)
    val bucketId: UUID = UUID.randomUUID(),
    @Column(name = "object_count", nullable = false)
    var objectCount: Long = 0,
    @Column(name = "stored_bytes", nullable = false)
    var storedBytes: Long = 0,
    @Column(name = "uploaded_bytes", nullable = false)
    var uploadedBytes: Long = 0,
    @Column(name = "downloaded_bytes", nullable = false)
    var downloadedBytes: Long = 0,
    @Column(name = "last_recorded_at", nullable = false)
    var lastRecordedAt: LocalDateTime = LocalDateTime.now(),
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
