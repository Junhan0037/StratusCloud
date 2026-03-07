package com.stratuscloud.iam.repository

import com.stratuscloud.iam.domain.SecretEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SecretRepository : JpaRepository<SecretEntity, UUID> {
    fun existsByTenantIdAndProjectIdAndName(tenantId: UUID, projectId: UUID?, name: String): Boolean
    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<SecretEntity>
    fun findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId: UUID, projectId: UUID): List<SecretEntity>
}
