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
@Table(name = "network_route_table_associations")
class NetworkRouteTableAssociationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(name = "route_table_id", nullable = false)
    val routeTableId: UUID = UUID.randomUUID(),
    @Column(name = "subnet_id", nullable = false)
    val subnetId: UUID = UUID.randomUUID(),
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
