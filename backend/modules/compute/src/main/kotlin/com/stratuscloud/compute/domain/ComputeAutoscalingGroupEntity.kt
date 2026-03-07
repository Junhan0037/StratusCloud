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
@Table(name = "compute_autoscaling_groups")
class ComputeAutoscalingGroupEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 100)
    val name: String = "",
    @Column(name = "image_id", nullable = false)
    val imageId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 20)
    val flavor: String = "",
    @Column(name = "min_instances", nullable = false)
    var minInstances: Int = 1,
    @Column(name = "max_instances", nullable = false)
    var maxInstances: Int = 1,
    @Column(name = "desired_instances", nullable = false)
    var desiredInstances: Int = 1,
    @Column(name = "cpu_scale_out_percent", nullable = false)
    var cpuScaleOutPercent: Int = 70,
    @Column(name = "cpu_scale_in_percent", nullable = false)
    var cpuScaleInPercent: Int = 25,
    @Column(name = "memory_scale_out_percent", nullable = false)
    var memoryScaleOutPercent: Int = 80,
    @Column(name = "memory_scale_in_percent", nullable = false)
    var memoryScaleInPercent: Int = 30,
    @Enumerated(EnumType.STRING)
    @Column(name = "health_policy", nullable = false, length = 20)
    var healthPolicy: ComputeHealthPolicy = ComputeHealthPolicy.RESTART,
    @Column(name = "failure_threshold", nullable = false)
    var failureThreshold: Int = 3,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ComputeAutoscalingGroupStatus = ComputeAutoscalingGroupStatus.ACTIVE,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
