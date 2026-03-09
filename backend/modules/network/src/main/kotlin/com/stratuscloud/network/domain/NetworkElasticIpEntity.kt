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
@Table(name = "network_elastic_ips")
class NetworkElasticIpEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),
    @Column(name = "project_id", nullable = false)
    val projectId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 100)
    val name: String = "",
    @Column(name = "public_ip", nullable = false, length = 30)
    val publicIp: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_status", nullable = false, length = 20)
    var allocationStatus: NetworkElasticIpAllocationStatus = NetworkElasticIpAllocationStatus.UNASSIGNED,
    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_target_type", length = 30)
    var attachmentTargetType: NetworkElasticIpAttachmentType? = null,
    @Column(name = "attachment_target_id")
    var attachmentTargetId: UUID? = null,
    createdBy: String = "system"
) : BaseAuditableEntity(createdBy = createdBy)
