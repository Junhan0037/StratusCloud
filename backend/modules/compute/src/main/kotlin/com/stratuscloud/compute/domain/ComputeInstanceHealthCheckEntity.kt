package com.stratuscloud.compute.domain

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
@Table(name = "compute_instance_health_checks")
class ComputeInstanceHealthCheckEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "instance_id", nullable = false, unique = true)
    val instanceId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ComputeHealthStatus = ComputeHealthStatus.UNKNOWN,
    @Column(name = "failure_count", nullable = false)
    var failureCount: Int = 0,
    @Column(columnDefinition = "text")
    var detail: String? = null,
    @Column(name = "checked_at", nullable = false)
    var checkedAt: LocalDateTime = LocalDateTime.now()
)
