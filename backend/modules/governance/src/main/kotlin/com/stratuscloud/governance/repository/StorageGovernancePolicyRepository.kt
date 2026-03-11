package com.stratuscloud.governance.repository

import com.stratuscloud.governance.domain.StorageGovernancePolicyEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StorageGovernancePolicyRepository : JpaRepository<StorageGovernancePolicyEntity, UUID> {
    fun findByProjectId(projectId: UUID): StorageGovernancePolicyEntity?
}
