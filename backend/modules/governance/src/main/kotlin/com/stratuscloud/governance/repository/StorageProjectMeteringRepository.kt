package com.stratuscloud.governance.repository

import com.stratuscloud.governance.domain.StorageProjectMeteringEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StorageProjectMeteringRepository : JpaRepository<StorageProjectMeteringEntity, UUID> {
    fun findByProjectId(projectId: UUID): StorageProjectMeteringEntity?
}
