package com.stratuscloud.compute.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "compute_instance_metrics")
class ComputeInstanceMetricEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "instance_id", nullable = false, unique = true)
    val instanceId: UUID = UUID.randomUUID(),
    @Column(name = "cpu_percent", nullable = false)
    var cpuPercent: Int = 0,
    @Column(name = "memory_percent", nullable = false)
    var memoryPercent: Int = 0,
    @Column(name = "reported_at", nullable = false)
    var reportedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "reported_by", nullable = false, length = 100)
    var reportedBy: String = "system"
)
