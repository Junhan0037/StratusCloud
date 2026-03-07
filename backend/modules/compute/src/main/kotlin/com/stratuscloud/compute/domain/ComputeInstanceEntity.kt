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
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "compute_instances")
class ComputeInstanceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(name = "image_id", nullable = false)
    val imageId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 100)
    val name: String = "",
    @Column(nullable = false, length = 20)
    var flavor: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ComputeInstanceStatus = ComputeInstanceStatus.PENDING,
    @Column(name = "user_data", columnDefinition = "text")
    val userData: String? = null,
    @Column(name = "last_transition_at", nullable = false)
    var lastTransitionAt: LocalDateTime = LocalDateTime.now(),
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
