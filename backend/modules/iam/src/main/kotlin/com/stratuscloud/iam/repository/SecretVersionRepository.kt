package com.stratuscloud.iam.repository

import com.stratuscloud.iam.domain.SecretVersionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SecretVersionRepository : JpaRepository<SecretVersionEntity, UUID> {
    fun findAllBySecretIdOrderByVersionDesc(secretId: UUID): List<SecretVersionEntity>
    fun findBySecretIdAndVersion(secretId: UUID, version: Int): SecretVersionEntity?
}
