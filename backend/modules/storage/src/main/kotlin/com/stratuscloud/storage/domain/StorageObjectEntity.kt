package com.stratuscloud.storage.domain

import com.stratuscloud.iam.domain.BaseAuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "storage_objects")
class StorageObjectEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(name = "bucket_id", nullable = false)
    val bucketId: UUID = UUID.randomUUID(),
    @Column(name = "object_key", nullable = false, length = 300)
    var key: String = "",
    @Column(name = "content_type", nullable = false, length = 120)
    var contentType: String = "application/octet-stream",
    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long = 0,
    @Column(nullable = false, length = 80)
    var etag: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var acl: StorageObjectAcl = StorageObjectAcl.PRIVATE,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
