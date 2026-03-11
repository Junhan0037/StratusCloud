package com.stratuscloud.governance.domain

import com.stratuscloud.iam.domain.BaseAuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "governance_storage_policies")
class StorageGovernancePolicyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(name = "max_bucket_count")
    var maxBucketCount: Int? = null,
    @Column(name = "max_object_count")
    var maxObjectCount: Long? = null,
    @Column(name = "max_total_bytes")
    var maxTotalBytes: Long? = null,
    @Column(name = "presign_per_minute")
    var presignPerMinute: Int? = null,
    @Column(name = "upload_per_minute")
    var uploadPerMinute: Int? = null,
    @Column(name = "download_per_minute")
    var downloadPerMinute: Int? = null,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
