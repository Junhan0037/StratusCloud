package com.stratuscloud.storage.domain

import com.stratuscloud.iam.domain.BaseAuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "storage_presigned_tokens")
class StoragePresignedTokenEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(name = "bucket_id", nullable = false)
    val bucketId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val operation: StoragePresignOperation = StoragePresignOperation.UPLOAD,
    @Column(name = "object_key", nullable = false, length = 300)
    val objectKey: String = "",
    @Column(name = "content_type", length = 120)
    val contentType: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val acl: StorageObjectAcl = StorageObjectAcl.PRIVATE,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "used_at")
    var usedAt: LocalDateTime? = null,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
