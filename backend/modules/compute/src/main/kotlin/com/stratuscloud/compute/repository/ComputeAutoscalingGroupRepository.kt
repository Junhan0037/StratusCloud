package com.stratuscloud.compute.repository

import com.stratuscloud.compute.domain.ComputeAutoscalingGroupEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ComputeAutoscalingGroupRepository : JpaRepository<ComputeAutoscalingGroupEntity, UUID> {
    fun findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId: UUID, projectId: UUID): List<ComputeAutoscalingGroupEntity>
}
