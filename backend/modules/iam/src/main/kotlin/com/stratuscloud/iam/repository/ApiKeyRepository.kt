package com.stratuscloud.iam.repository

import com.stratuscloud.iam.domain.ApiKeyEntity
import com.stratuscloud.iam.domain.ApiKeyStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApiKeyRepository : JpaRepository<ApiKeyEntity, UUID> {
    fun findByKeyPrefixAndStatus(keyPrefix: String, status: ApiKeyStatus): ApiKeyEntity?
    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<ApiKeyEntity>
}
