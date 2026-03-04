package com.stratuscloud.iam.repository

import com.stratuscloud.iam.domain.TenantEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TenantRepository : JpaRepository<TenantEntity, UUID>
