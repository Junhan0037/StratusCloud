package com.stratuscloud.network.repository

import com.stratuscloud.network.domain.NetworkElasticIpAttachmentType
import com.stratuscloud.network.domain.NetworkElasticIpEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkElasticIpRepository : JpaRepository<NetworkElasticIpEntity, UUID> {
    fun existsByProjectIdAndName(projectId: UUID, name: String): Boolean
    fun existsByPublicIp(publicIp: String): Boolean
    fun findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId: UUID, projectId: UUID): List<NetworkElasticIpEntity>
    fun existsByAttachmentTargetTypeAndAttachmentTargetId(targetType: NetworkElasticIpAttachmentType, targetId: UUID): Boolean
    fun findByAttachmentTargetTypeAndAttachmentTargetId(
        targetType: NetworkElasticIpAttachmentType,
        targetId: UUID
    ): NetworkElasticIpEntity?
}
