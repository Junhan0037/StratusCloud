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
@Table(name = "network_load_balancers")
class NetworkLoadBalancerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(name = "vpc_id", nullable = false)
    val vpcId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 100)
    val name: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: NetworkLoadBalancerType = NetworkLoadBalancerType.L4,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val scheme: NetworkLoadBalancerScheme = NetworkLoadBalancerScheme.INTERNAL,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
