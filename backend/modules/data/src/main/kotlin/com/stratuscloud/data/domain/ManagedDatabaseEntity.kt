package com.stratuscloud.data.domain

import com.stratuscloud.iam.domain.BaseAuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "managed_databases")
class ManagedDatabaseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 100)
    var name: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var engine: ManagedDatabaseEngine = ManagedDatabaseEngine.POSTGRESQL,
    @Column(name = "engine_version", nullable = false, length = 30)
    var engineVersion: String = "",
    @Column(name = "instance_class", nullable = false, length = 40)
    var instanceClass: String = "",
    @Column(name = "storage_gb", nullable = false)
    var storageGb: Int = 20,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: ManagedDatabaseStatus = ManagedDatabaseStatus.AVAILABLE,
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
