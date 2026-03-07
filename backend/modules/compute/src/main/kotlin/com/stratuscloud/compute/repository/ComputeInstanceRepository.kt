package com.stratuscloud.compute.repository

import com.stratuscloud.compute.domain.ComputeInstanceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ComputeInstanceRepository : JpaRepository<ComputeInstanceEntity, UUID> {
    fun findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId: UUID, projectId: UUID): List<ComputeInstanceEntity>
    fun findAllByAutoscalingGroupIdOrderByCreatedAtDesc(autoscalingGroupId: UUID): List<ComputeInstanceEntity>
}
