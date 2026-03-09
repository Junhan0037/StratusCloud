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
@Table(name = "network_dns_records")
class NetworkDnsRecordEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 200)
    val name: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 10)
    val recordType: NetworkDnsRecordType = NetworkDnsRecordType.A,
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    val targetType: NetworkDnsTargetType = NetworkDnsTargetType.LOAD_BALANCER,
    @Column(name = "target_id", nullable = false)
    val targetId: UUID = UUID.randomUUID(),
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
