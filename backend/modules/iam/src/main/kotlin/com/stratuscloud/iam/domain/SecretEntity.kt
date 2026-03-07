package com.stratuscloud.iam.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "iam_secrets")
class SecretEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = true)
    val projectId: UUID? = null,
    @Column(nullable = false, length = 100)
    val name: String = "",
    @Column(name = "latest_version", nullable = false)
    var latestVersion: Int = 1,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
