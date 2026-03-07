package com.stratuscloud.compute.repository

import com.stratuscloud.compute.domain.ComputeInstanceHealthCheckEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ComputeInstanceHealthCheckRepository : JpaRepository<ComputeInstanceHealthCheckEntity, UUID> {
    fun findByInstanceId(instanceId: UUID): ComputeInstanceHealthCheckEntity?
    fun findAllByInstanceIdIn(instanceIds: Collection<UUID>): List<ComputeInstanceHealthCheckEntity>
}
