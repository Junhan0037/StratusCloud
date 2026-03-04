package com.stratuscloud.iam.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "iam_users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(nullable = false, length = 255)
    val email: String = "",
    @Column(name = "display_name", nullable = false, length = 100)
    val displayName: String = "",
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
