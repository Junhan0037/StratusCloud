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
@Table(name = "network_route_tables")
class NetworkRouteTableEntity(
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
    var name: String = "",
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
