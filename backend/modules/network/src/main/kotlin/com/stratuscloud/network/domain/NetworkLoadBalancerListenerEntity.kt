package com.stratuscloud.network.domain

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
@Table(name = "network_load_balancer_listeners")
class NetworkLoadBalancerListenerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(name = "load_balancer_id", nullable = false)
    val loadBalancerId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val protocol: NetworkLoadBalancerProtocol = NetworkLoadBalancerProtocol.TCP,
    @Column(nullable = false)
    val port: Int = 80,
    @Column(name = "default_target_subnet_id", nullable = false)
    val defaultTargetSubnetId: UUID = UUID.randomUUID(),
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
