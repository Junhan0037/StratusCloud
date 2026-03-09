package com.stratuscloud.network.domain

import com.stratuscloud.iam.domain.BaseAuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "network_load_balancer_rules")
class NetworkLoadBalancerRuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(name = "listener_id", nullable = false)
    val listenerId: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val priority: Int = 1,
    @Column(name = "path_pattern", nullable = false, length = 200)
    val pathPattern: String = "",
    @Column(name = "target_subnet_id", nullable = false)
    val targetSubnetId: UUID = UUID.randomUUID(),
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
