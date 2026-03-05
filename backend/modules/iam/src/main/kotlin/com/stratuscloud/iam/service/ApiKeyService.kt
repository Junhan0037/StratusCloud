package com.stratuscloud.iam.service

import com.stratuscloud.iam.domain.ApiKeyEntity
import com.stratuscloud.iam.domain.ApiKeyStatus
import com.stratuscloud.iam.domain.RoleType
import com.stratuscloud.iam.exception.BadRequestException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import com.stratuscloud.iam.exception.UnauthorizedException
import com.stratuscloud.iam.repository.ApiKeyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.UUID

data class IssuedApiKey(
    val entity: ApiKeyEntity,
    val rawKey: String
)

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
    private val apiKeySecretHasher: ApiKeySecretHasher
) {

    @Transactional
    fun issueApiKey(
        tenantId: UUID,
        projectId: UUID?,
        name: String,
        role: RoleType,
        requestedExpiresAt: LocalDateTime?,
        actorId: UUID
    ): IssuedApiKey {
        val now = LocalDateTime.now()
        val defaultExpiresAt = now.plusDays(DEFAULT_TTL_DAYS)
        val expiresAt = requestedExpiresAt ?: defaultExpiresAt
        if (expiresAt.isBefore(now.plusMinutes(1))) {
            throw BadRequestException("expiresAt must be in the future")
        }
        if (expiresAt.isAfter(defaultExpiresAt)) {
            throw BadRequestException("expiresAt cannot exceed default 90 days window")
        }

        val rawSecret = generateSecret()
        val prefix = rawSecret.take(PREFIX_LENGTH)
        val rawKey = "$KEY_PREFIX$prefix.$rawSecret"

        val entity = apiKeyRepository.save(
            ApiKeyEntity(
                tenantId = tenantId,
                projectId = projectId,
                name = name.trim(),
                role = role,
                keyPrefix = prefix,
                secretHash = apiKeySecretHasher.hash(rawSecret),
                status = ApiKeyStatus.ACTIVE,
                expiresAt = expiresAt,
                createdBy = actorId.toString()
            )
        )
        return IssuedApiKey(entity = entity, rawKey = rawKey)
    }

    @Transactional
    fun revokeApiKey(keyId: UUID): ApiKeyEntity {
        val entity = getApiKey(keyId)
        entity.status = ApiKeyStatus.REVOKED
        entity.revokedAt = LocalDateTime.now()
        return apiKeyRepository.save(entity)
    }

    @Transactional(readOnly = true)
    fun getApiKey(keyId: UUID): ApiKeyEntity {
        return apiKeyRepository.findById(keyId)
            .orElseThrow { ResourceNotFoundException("api key not found: $keyId") }
    }

    @Transactional(readOnly = true)
    fun listApiKeys(tenantId: UUID): List<ApiKeyEntity> {
        return apiKeyRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
    }

    @Transactional
    fun authenticate(rawKey: String): ApiKeyEntity {
        val parsed = parseRawKey(rawKey)
        val entity = apiKeyRepository.findByKeyPrefixAndStatus(parsed.prefix, ApiKeyStatus.ACTIVE)
            ?: throw UnauthorizedException("api key is invalid")

        if (entity.expiresAt.isBefore(LocalDateTime.now())) {
            throw UnauthorizedException("api key is expired")
        }

        val hash = apiKeySecretHasher.hash(parsed.secret)
        if (hash != entity.secretHash) {
            throw UnauthorizedException("api key is invalid")
        }

        entity.lastUsedAt = LocalDateTime.now()
        return apiKeyRepository.save(entity)
    }

    private fun parseRawKey(rawKey: String): ParsedKey {
        if (!rawKey.startsWith(KEY_PREFIX)) {
            throw UnauthorizedException("api key format is invalid")
        }
        val body = rawKey.removePrefix(KEY_PREFIX)
        val separatorIndex = body.indexOf('.')
        if (separatorIndex <= 0 || separatorIndex == body.lastIndex) {
            throw UnauthorizedException("api key format is invalid")
        }
        val prefix = body.substring(0, separatorIndex)
        val secret = body.substring(separatorIndex + 1)
        if (prefix.length != PREFIX_LENGTH || secret.length < 32) {
            throw UnauthorizedException("api key format is invalid")
        }
        return ParsedKey(prefix = prefix, secret = secret)
    }

    private fun generateSecret(): String {
        // 충돌 가능성을 낮추기 위해 48바이트 난수를 hex 문자열로 인코딩한다.
        val bytes = ByteArray(48)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private data class ParsedKey(
        val prefix: String,
        val secret: String
    )

    companion object {
        private const val DEFAULT_TTL_DAYS = 90L
        private const val PREFIX_LENGTH = 12
        private const val KEY_PREFIX = "sc_"
        private val secureRandom = SecureRandom()
    }
}
