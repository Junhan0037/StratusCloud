package com.stratuscloud.iam.repository

import com.stratuscloud.iam.domain.ProjectEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectRepository : JpaRepository<ProjectEntity, UUID> {
    fun existsByTenantIdAndName(tenantId: UUID, name: String): Boolean
}
