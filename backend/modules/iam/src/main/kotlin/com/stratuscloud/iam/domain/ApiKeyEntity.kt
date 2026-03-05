package com.stratuscloud.iam.domain

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
@Table(name = "iam_api_keys")
class ApiKeyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = true)
    val projectId: UUID? = null,
    @Column(nullable = false, length = 100)
    val name: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val role: RoleType = RoleType.VIEWER,
    @Column(name = "key_prefix", nullable = false, length = 20)
    val keyPrefix: String = "",
    @Column(name = "secret_hash", nullable = false, length = 128)
    val secretHash: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ApiKeyStatus = ApiKeyStatus.ACTIVE,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "revoked_at", nullable = true)
    var revokedAt: LocalDateTime? = null,
    @Column(name = "last_used_at", nullable = true)
    var lastUsedAt: LocalDateTime? = null,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
