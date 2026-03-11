package com.stratuscloud.storage.service

import com.stratuscloud.governance.service.StorageGovernanceService
import com.stratuscloud.governance.service.StorageRateLimitOperation
import com.stratuscloud.iam.exception.BadRequestException
import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import com.stratuscloud.iam.exception.UnauthorizedException
import com.stratuscloud.iam.repository.ProjectRepository
import com.stratuscloud.storage.domain.StorageBucketEntity
import com.stratuscloud.storage.domain.StorageObjectAcl
import com.stratuscloud.storage.domain.StorageObjectEntity
import com.stratuscloud.storage.domain.StoragePresignOperation
import com.stratuscloud.storage.domain.StoragePresignedTokenEntity
import com.stratuscloud.storage.repository.StorageBucketRepository
import com.stratuscloud.storage.repository.StorageObjectRepository
import com.stratuscloud.storage.repository.StoragePresignedTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.HexFormat
import java.util.UUID

@Service
class StorageService(
    private val projectRepository: ProjectRepository,
    private val bucketRepository: StorageBucketRepository,
    private val objectRepository: StorageObjectRepository,
    private val presignedTokenRepository: StoragePresignedTokenRepository,
    private val localObjectStorage: LocalObjectStorage,
    private val storageGovernanceService: StorageGovernanceService
) {

    @Transactional
    fun createBucket(
        tenantId: UUID,
        projectId: UUID,
        name: String,
        acl: StorageObjectAcl,
        actorId: UUID
    ): StorageBucketEntity {
        validateProjectScope(tenantId, projectId)
        val normalizedName = name.trim()
        if (bucketRepository.existsByProjectIdAndName(projectId, normalizedName)) {
            throw DuplicateResourceException("bucket already exists: $normalizedName")
        }
        storageGovernanceService.assertBucketQuota(projectId, bucketRepository.countByProjectId(projectId) + 1)
        val created = bucketRepository.save(
            StorageBucketEntity(
                tenantId = tenantId,
                projectId = projectId,
                name = normalizedName,
                acl = acl,
                createdBy = actorId.toString()
            )
        )
        storageGovernanceService.recordBucketCreated(tenantId, projectId, actorId.toString())
        return created
    }

    @Transactional(readOnly = true)
    fun listBuckets(tenantId: UUID, projectId: UUID): List<StorageBucketEntity> {
        return bucketRepository.findAllByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId)
    }

    @Transactional(readOnly = true)
    fun getBucket(bucketId: UUID): StorageBucketEntity {
        return bucketRepository.findById(bucketId)
            .orElseThrow { ResourceNotFoundException("bucket not found: $bucketId") }
    }

    @Transactional(readOnly = true)
    fun countObjects(bucketId: UUID): Long {
        return objectRepository.countByBucketId(bucketId)
    }

    @Transactional
    fun deleteBucket(bucketId: UUID) {
        val bucket = getBucket(bucketId)
        if (objectRepository.countByBucketId(bucketId) > 0) {
            throw BadRequestException("cannot delete bucket with objects: $bucketId")
        }
        presignedTokenRepository.deleteAllByBucketId(bucketId)
        storageGovernanceService.recordBucketDeleted(bucket.tenantId, bucket.projectId, bucketId, bucket.createdBy)
        bucketRepository.delete(bucket)
    }

    @Transactional(readOnly = true)
    fun listObjects(bucketId: UUID): List<StorageObjectEntity> {
        getBucket(bucketId)
        return objectRepository.findAllByBucketIdOrderByCreatedAtDesc(bucketId)
    }

    @Transactional(readOnly = true)
    fun getObject(objectId: UUID): StorageObjectEntity {
        return objectRepository.findById(objectId)
            .orElseThrow { ResourceNotFoundException("object not found: $objectId") }
    }

    @Transactional
    fun deleteObject(objectId: UUID) {
        val entity = getObject(objectId)
        localObjectStorage.deleteObject(requireNotNull(entity.id) { "object id is null" })
        objectRepository.delete(entity)
        storageGovernanceService.recordObjectDeleted(
            tenantId = entity.tenantId,
            projectId = entity.projectId,
            bucketId = entity.bucketId,
            sizeBytes = entity.sizeBytes,
            actorId = entity.createdBy
        )
    }

    @Transactional
    fun createPresignedToken(
        tenantId: UUID,
        projectId: UUID,
        bucketId: UUID,
        operation: StoragePresignOperation,
        key: String,
        contentType: String?,
        acl: StorageObjectAcl?,
        expiresInSeconds: Int,
        actorId: UUID
    ): StoragePresignedTokenEntity {
        if (expiresInSeconds !in 0..3600) {
            throw BadRequestException("expiresInSeconds must be between 0 and 3600")
        }
        val bucket = getBucket(bucketId)
        requireScope(bucket.tenantId == tenantId && bucket.projectId == projectId, "bucket scope mismatch: $bucketId")
        storageGovernanceService.assertRateLimit(projectId, StorageRateLimitOperation.PRESIGN)
        val normalizedKey = normalizeKey(key)
        if (operation == StoragePresignOperation.UPLOAD && objectRepository.existsByBucketIdAndKey(bucketId, normalizedKey)) {
            throw DuplicateResourceException("object already exists in bucket: $normalizedKey")
        }
        if (operation == StoragePresignOperation.DOWNLOAD) {
            objectRepository.findByBucketIdAndKey(bucketId, normalizedKey)
                ?: throw ResourceNotFoundException("object not found in bucket: $normalizedKey")
        }
        return presignedTokenRepository.save(
            StoragePresignedTokenEntity(
                tenantId = tenantId,
                projectId = projectId,
                bucketId = bucketId,
                operation = operation,
                objectKey = normalizedKey,
                contentType = contentType?.trim()?.takeIf { it.isNotBlank() },
                acl = acl ?: bucket.acl,
                expiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds.toLong()),
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional
    fun uploadObject(tokenId: UUID, content: ByteArray, requestContentType: String?): StorageObjectEntity {
        val token = resolveToken(tokenId, StoragePresignOperation.UPLOAD)
        if (token.usedAt != null) {
            throw UnauthorizedException("presigned url already consumed")
        }
        storageGovernanceService.assertRateLimit(token.projectId, StorageRateLimitOperation.UPLOAD)
        if (objectRepository.existsByBucketIdAndKey(token.bucketId, token.objectKey)) {
            throw DuplicateResourceException("object already exists in bucket: ${token.objectKey}")
        }
        storageGovernanceService.assertObjectUploadQuota(
            projectId = token.projectId,
            nextObjectCount = objectRepository.countByProjectId(token.projectId) + 1,
            nextStoredBytes = objectRepository.sumSizeBytesByProjectId(token.projectId) + content.size.toLong()
        )
        val entity = objectRepository.save(
            StorageObjectEntity(
                tenantId = token.tenantId,
                projectId = token.projectId,
                bucketId = token.bucketId,
                key = token.objectKey,
                contentType = normalizeContentType(token.contentType ?: requestContentType),
                sizeBytes = content.size.toLong(),
                etag = digest(content),
                acl = token.acl,
                createdBy = token.createdBy
            )
        )
        localObjectStorage.writeObject(requireNotNull(entity.id) { "object id is null" }, content)
        token.usedAt = LocalDateTime.now()
        presignedTokenRepository.save(token)
        storageGovernanceService.recordObjectUploaded(
            tenantId = token.tenantId,
            projectId = token.projectId,
            bucketId = token.bucketId,
            sizeBytes = entity.sizeBytes,
            actorId = token.createdBy
        )
        return entity
    }

    @Transactional
    fun downloadObject(tokenId: UUID): Pair<StorageObjectEntity, ByteArray> {
        val token = resolveToken(tokenId, StoragePresignOperation.DOWNLOAD)
        storageGovernanceService.assertRateLimit(token.projectId, StorageRateLimitOperation.DOWNLOAD)
        val entity = objectRepository.findByBucketIdAndKey(token.bucketId, token.objectKey)
            ?: throw UnauthorizedException("presigned object is missing")
        val body = localObjectStorage.readObject(requireNotNull(entity.id) { "object id is null" })
        storageGovernanceService.recordObjectDownloaded(
            tenantId = token.tenantId,
            projectId = token.projectId,
            bucketId = token.bucketId,
            sizeBytes = entity.sizeBytes,
            actorId = token.createdBy
        )
        return entity to body
    }

    private fun resolveToken(tokenId: UUID, operation: StoragePresignOperation): StoragePresignedTokenEntity {
        val token = presignedTokenRepository.findById(tokenId)
            .orElseThrow { UnauthorizedException("invalid presigned url") }
        if (token.operation != operation) {
            throw UnauthorizedException("invalid presigned url")
        }
        if (!token.expiresAt.isAfter(LocalDateTime.now())) {
            throw UnauthorizedException("presigned url expired")
        }
        return token
    }

    private fun validateProjectScope(tenantId: UUID, projectId: UUID) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ResourceNotFoundException("project not found: $projectId") }
        if (project.tenantId != tenantId) {
            throw BadRequestException("project does not belong to tenant: $projectId")
        }
    }

    private fun normalizeKey(key: String): String {
        val normalized = key.trim().trim('/')
        if (normalized.isBlank()) {
            throw BadRequestException("object key must not be blank")
        }
        return normalized
    }

    private fun normalizeContentType(contentType: String?): String {
        return contentType?.substringBefore(';')?.trim()?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
    }

    private fun digest(content: ByteArray): String {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))
    }

    private fun requireScope(condition: Boolean, message: String) {
        if (!condition) {
            throw BadRequestException(message)
        }
    }
}
