package com.stratuscloud.compute.domain

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
@Table(name = "compute_images")
class ComputeImageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 100)
    val name: String = "",
    @Column(nullable = false, length = 50)
    val version: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "os_type", nullable = false, length = 20)
    val osType: ComputeOsType = ComputeOsType.LINUX,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ComputeImageStatus = ComputeImageStatus.ACTIVE,
    @Column(nullable = false, columnDefinition = "text")
    var tags: String = "",
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
