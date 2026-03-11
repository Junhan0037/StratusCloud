package com.stratuscloud.governance.domain

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
@Table(name = "governance_storage_tags")
class StorageTagEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    val resourceType: StorageTagResourceType = StorageTagResourceType.BUCKET,
    @Column(name = "resource_id", nullable = false)
    val resourceId: UUID = UUID.randomUUID(),
    @Column(name = "tag_value", nullable = false, length = 80)
    val tagValue: String = "",
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
