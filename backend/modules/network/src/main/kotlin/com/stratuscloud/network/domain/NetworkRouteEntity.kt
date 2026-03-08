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
@Table(name = "network_routes")
class NetworkRouteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(name = "vpc_id", nullable = false)
    val vpcId: UUID = UUID.randomUUID(),
    @Column(name = "route_table_id", nullable = false)
    val routeTableId: UUID = UUID.randomUUID(),
    @Column(name = "destination_cidr", nullable = false, length = 32)
    var destinationCidr: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    var targetType: NetworkRouteTargetType = NetworkRouteTargetType.LOCAL,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
