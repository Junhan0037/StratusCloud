package com.stratuscloud.iam.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "iam_policies")
class PolicyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 100)
    val name: String = "",
    @Column(nullable = true, length = 300)
    val description: String? = null,
    @Column(name = "document", nullable = false, columnDefinition = "text")
    val document: String = "",
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
