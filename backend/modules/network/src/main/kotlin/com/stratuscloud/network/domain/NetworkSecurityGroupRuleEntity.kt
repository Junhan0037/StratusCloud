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
@Table(name = "network_security_group_rules")
class NetworkSecurityGroupRuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(name = "security_group_id", nullable = false)
    val securityGroupId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var direction: NetworkRuleDirection = NetworkRuleDirection.INGRESS,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var protocol: NetworkRuleProtocol = NetworkRuleProtocol.TCP,
    @Column(name = "port_range_start")
    var portRangeStart: Int? = null,
    @Column(name = "port_range_end")
    var portRangeEnd: Int? = null,
    @Column(name = "cidr_block", nullable = false, length = 32)
    var cidrBlock: String = "",
    @Column(nullable = true, length = 300)
    var description: String? = null,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
