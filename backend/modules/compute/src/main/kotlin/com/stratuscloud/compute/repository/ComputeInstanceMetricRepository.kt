package com.stratuscloud.compute.repository

import com.stratuscloud.compute.domain.ComputeInstanceMetricEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ComputeInstanceMetricRepository : JpaRepository<ComputeInstanceMetricEntity, UUID> {
    fun findByInstanceId(instanceId: UUID): ComputeInstanceMetricEntity?
    fun findAllByInstanceIdIn(instanceIds: Collection<UUID>): List<ComputeInstanceMetricEntity>
}
