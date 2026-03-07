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
@Table(name = "iam_secret_versions")
class SecretVersionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "secret_id", nullable = false)
    val secretId: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val version: Int = 1,
    @Column(name = "value_ciphertext", nullable = false, columnDefinition = "text")
    val valueCiphertext: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SecretVersionStatus = SecretVersionStatus.ACTIVE,
    @Column(name = "revoked_at", nullable = true)
    var revokedAt: LocalDateTime? = null,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
