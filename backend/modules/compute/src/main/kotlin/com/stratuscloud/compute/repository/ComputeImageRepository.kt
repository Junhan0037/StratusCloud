package com.stratuscloud.compute.repository

import com.stratuscloud.compute.domain.ComputeImageEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ComputeImageRepository : JpaRepository<ComputeImageEntity, UUID> {
    fun existsByTenantIdAndNameAndVersion(tenantId: UUID, name: String, version: String): Boolean
    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<ComputeImageEntity>
}
