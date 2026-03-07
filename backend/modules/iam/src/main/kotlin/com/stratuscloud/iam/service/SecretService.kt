package com.stratuscloud.iam.service

import com.stratuscloud.iam.domain.SecretEntity
import com.stratuscloud.iam.domain.SecretVersionEntity
import com.stratuscloud.iam.domain.SecretVersionStatus
import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import com.stratuscloud.iam.repository.SecretRepository
import com.stratuscloud.iam.repository.SecretVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

data class SecretRecord(
    val secret: SecretEntity,
    val currentVersion: SecretVersionEntity
)

@Service
class SecretService(
    private val secretRepository: SecretRepository,
    private val secretVersionRepository: SecretVersionRepository,
    private val secretCryptoService: SecretCryptoService
) {

    @Transactional
    fun createSecret(
        tenantId: UUID,
        projectId: UUID?,
        name: String,
        value: String,
        actorId: UUID
    ): SecretRecord {
        val normalizedName = name.trim()
        if (secretRepository.existsByTenantIdAndProjectIdAndName(tenantId, projectId, normalizedName)) {
            throw DuplicateResourceException("secret already exists: $normalizedName")
        }

        val savedSecret = secretRepository.save(
            SecretEntity(
                tenantId = tenantId,
                projectId = projectId,
                name = normalizedName,
                latestVersion = 1,
                createdBy = actorId.toString()
            )
        )
        val secretId = requireNotNull(savedSecret.id) { "secret id is null" }
        val savedVersion = secretVersionRepository.save(
            SecretVersionEntity(
                secretId = secretId,
                version = 1,
                valueCiphertext = secretCryptoService.encrypt(value),
                status = SecretVersionStatus.ACTIVE,
                createdBy = actorId.toString()
            )
        )
        return SecretRecord(secret = savedSecret, currentVersion = savedVersion)
    }

    @Transactional(readOnly = true)
    fun listSecrets(tenantId: UUID, projectId: UUID?): List<SecretEntity> {
        return if (projectId == null) {
            secretRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
        } else {
            secretRepository.findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId)
        }
    }

    @Transactional(readOnly = true)
    fun getSecret(secretId: UUID): SecretEntity {
        return secretRepository.findById(secretId)
            .orElseThrow { ResourceNotFoundException("secret not found: $secretId") }
    }

    @Transactional(readOnly = true)
    fun listVersions(secretId: UUID): List<SecretVersionEntity> {
        getSecret(secretId)
        return secretVersionRepository.findAllBySecretIdOrderByVersionDesc(secretId)
    }

    @Transactional
    fun rotateSecret(secretId: UUID, value: String, actorId: UUID): SecretRecord {
        val secret = getSecret(secretId)
        val nextVersion = secret.latestVersion + 1
        secret.latestVersion = nextVersion
        val updatedSecret = secretRepository.save(secret)
        val savedVersion = secretVersionRepository.save(
            SecretVersionEntity(
                secretId = requireNotNull(updatedSecret.id),
                version = nextVersion,
                valueCiphertext = secretCryptoService.encrypt(value),
                status = SecretVersionStatus.ACTIVE,
                createdBy = actorId.toString()
            )
        )
        return SecretRecord(secret = updatedSecret, currentVersion = savedVersion)
    }

    @Transactional
    fun revokeVersion(secretId: UUID, versionId: UUID): SecretVersionEntity {
        getSecret(secretId)
        val version = secretVersionRepository.findById(versionId)
            .orElseThrow { ResourceNotFoundException("secret version not found: $versionId") }
        if (version.secretId != secretId) {
            throw ResourceNotFoundException("secret version not found: $versionId")
        }
        version.status = SecretVersionStatus.REVOKED
        version.revokedAt = LocalDateTime.now()
        return secretVersionRepository.save(version)
    }
}
