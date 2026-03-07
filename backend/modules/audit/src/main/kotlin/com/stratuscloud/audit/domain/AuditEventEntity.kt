package com.stratuscloud.audit.domain

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
@Table(name = "audit_events")
class AuditEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "trace_id", nullable = false, length = 64)
    val traceId: String = "",
    @Column(name = "actor_id", nullable = false)
    val actorId: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = true)
    val projectId: UUID? = null,
    @Column(nullable = false, length = 100)
    val action: String = "",
    @Column(name = "resource_type", nullable = false, length = 40)
    val resourceType: String = "",
    @Column(name = "resource_id", nullable = true, length = 120)
    val resourceId: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val result: AuditResult = AuditResult.SUCCESS,
    @Column(nullable = false, columnDefinition = "text")
    val metadata: String = "{}",
    @Column(name = "occurred_at", nullable = false)
    val occurredAt: LocalDateTime = LocalDateTime.now()
)
