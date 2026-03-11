package com.stratuscloud.governance.service

import com.stratuscloud.governance.domain.StorageBucketMeteringEntity
import com.stratuscloud.governance.domain.StorageGovernancePolicyEntity
import com.stratuscloud.governance.domain.StorageProjectMeteringEntity
import com.stratuscloud.governance.domain.StorageTagEntity
import com.stratuscloud.governance.domain.StorageTagResourceType
import com.stratuscloud.governance.repository.StorageBucketMeteringRepository
import com.stratuscloud.governance.repository.StorageGovernancePolicyRepository
import com.stratuscloud.governance.repository.StorageProjectMeteringRepository
import com.stratuscloud.governance.repository.StorageTagRepository
import com.stratuscloud.iam.exception.BadRequestException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import com.stratuscloud.iam.exception.TooManyRequestsException
import com.stratuscloud.iam.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class StoragePolicySnapshot(
    val tenantId: UUID,
    val projectId: UUID,
    val maxBucketCount: Int?,
    val maxObjectCount: Long?,
    val maxTotalBytes: Long?,
    val presignPerMinute: Int?,
    val uploadPerMinute: Int?,
    val downloadPerMinute: Int?
)

data class StorageProjectMeteringSnapshot(
    val tenantId: UUID,
    val projectId: UUID,
    val bucketCount: Long,
    val objectCount: Long,
    val storedBytes: Long,
    val uploadedBytes: Long,
    val downloadedBytes: Long,
    val lastRecordedAt: LocalDateTime
)

data class StorageBucketMeteringSnapshot(
    val tenantId: UUID,
    val projectId: UUID,
    val bucketId: UUID,
    val objectCount: Long,
    val storedBytes: Long,
    val uploadedBytes: Long,
    val downloadedBytes: Long,
    val lastRecordedAt: LocalDateTime
)

enum class StorageRateLimitOperation {
    PRESIGN,
    UPLOAD,
    DOWNLOAD
}

@Service
class StorageGovernanceService(
    private val projectRepository: ProjectRepository,
    private val policyRepository: StorageGovernancePolicyRepository,
    private val tagRepository: StorageTagRepository,
    private val projectMeteringRepository: StorageProjectMeteringRepository,
    private val bucketMeteringRepository: StorageBucketMeteringRepository
) {
    private val requestWindows = ConcurrentHashMap<RateLimitKey, FixedWindowCounter>()

    @Transactional
    fun upsertPolicy(
        tenantId: UUID,
        projectId: UUID,
        maxBucketCount: Int?,
        maxObjectCount: Long?,
        maxTotalBytes: Long?,
        presignPerMinute: Int?,
        uploadPerMinute: Int?,
        downloadPerMinute: Int?,
        actorId: UUID
    ): StorageGovernancePolicyEntity {
        validateProjectScope(tenantId, projectId)
        requirePositiveOrNull(maxBucketCount?.toLong(), "maxBucketCount")
        requirePositiveOrNull(maxObjectCount, "maxObjectCount")
        requirePositiveOrNull(maxTotalBytes, "maxTotalBytes")
        requirePositiveOrNull(presignPerMinute?.toLong(), "presignPerMinute")
        requirePositiveOrNull(uploadPerMinute?.toLong(), "uploadPerMinute")
        requirePositiveOrNull(downloadPerMinute?.toLong(), "downloadPerMinute")

        val entity = policyRepository.findByProjectId(projectId)
            ?: StorageGovernancePolicyEntity(
                tenantId = tenantId,
                projectId = projectId,
                createdBy = actorId.toString()
            )
        entity.maxBucketCount = maxBucketCount
        entity.maxObjectCount = maxObjectCount
        entity.maxTotalBytes = maxTotalBytes
        entity.presignPerMinute = presignPerMinute
        entity.uploadPerMinute = uploadPerMinute
        entity.downloadPerMinute = downloadPerMinute
        return policyRepository.save(entity)
    }

    @Transactional(readOnly = true)
    fun getPolicySnapshot(tenantId: UUID, projectId: UUID): StoragePolicySnapshot {
        validateProjectScope(tenantId, projectId)
        val entity = policyRepository.findByProjectId(projectId)
        return StoragePolicySnapshot(
            tenantId = tenantId,
            projectId = projectId,
            maxBucketCount = entity?.maxBucketCount,
            maxObjectCount = entity?.maxObjectCount,
            maxTotalBytes = entity?.maxTotalBytes,
            presignPerMinute = entity?.presignPerMinute,
            uploadPerMinute = entity?.uploadPerMinute,
            downloadPerMinute = entity?.downloadPerMinute
        )
    }

    @Transactional(readOnly = true)
    fun getProjectMetering(tenantId: UUID, projectId: UUID): StorageProjectMeteringSnapshot {
        validateProjectScope(tenantId, projectId)
        val entity = projectMeteringRepository.findByProjectId(projectId)
        return entity?.toSnapshot() ?: StorageProjectMeteringSnapshot(
            tenantId = tenantId,
            projectId = projectId,
            bucketCount = 0,
            objectCount = 0,
            storedBytes = 0,
            uploadedBytes = 0,
            downloadedBytes = 0,
            lastRecordedAt = LocalDateTime.now()
        )
    }

    @Transactional(readOnly = true)
    fun getBucketMetering(tenantId: UUID, projectId: UUID, bucketId: UUID): StorageBucketMeteringSnapshot {
        validateProjectScope(tenantId, projectId)
        val entity = bucketMeteringRepository.findByBucketId(bucketId)
        return entity?.toSnapshot() ?: StorageBucketMeteringSnapshot(
            tenantId = tenantId,
            projectId = projectId,
            bucketId = bucketId,
            objectCount = 0,
            storedBytes = 0,
            uploadedBytes = 0,
            downloadedBytes = 0,
            lastRecordedAt = LocalDateTime.now()
        )
    }

    @Transactional
    fun replaceTags(
        tenantId: UUID,
        projectId: UUID,
        resourceType: StorageTagResourceType,
        resourceId: UUID,
        tags: List<String>,
        actorId: UUID
    ): List<String> {
        validateProjectScope(tenantId, projectId)
        tagRepository.deleteAllByResourceTypeAndResourceId(resourceType, resourceId)
        val normalized = normalizeTags(tags)
        if (normalized.isNotEmpty()) {
            tagRepository.saveAll(
                normalized.map { tag ->
                    StorageTagEntity(
                        tenantId = tenantId,
                        projectId = projectId,
                        resourceType = resourceType,
                        resourceId = resourceId,
                        tagValue = tag,
                        createdBy = actorId.toString()
                    )
                }
            )
        }
        return normalized
    }

    @Transactional(readOnly = true)
    fun listTags(
        tenantId: UUID,
        projectId: UUID,
        resourceType: StorageTagResourceType,
        resourceId: UUID
    ): List<String> {
        validateProjectScope(tenantId, projectId)
        return tagRepository.findAllByResourceTypeAndResourceIdOrderByCreatedAtAsc(resourceType, resourceId)
            .map(StorageTagEntity::tagValue)
    }

    @Transactional(readOnly = true)
    fun assertBucketQuota(projectId: UUID, nextBucketCount: Long) {
        val policy = policyRepository.findByProjectId(projectId) ?: return
        val limit = policy.maxBucketCount?.toLong() ?: return
        if (nextBucketCount > limit) {
            throw BadRequestException("bucket quota exceeded for project: $projectId")
        }
    }

    @Transactional(readOnly = true)
    fun assertObjectUploadQuota(projectId: UUID, nextObjectCount: Long, nextStoredBytes: Long) {
        val policy = policyRepository.findByProjectId(projectId) ?: return
        policy.maxObjectCount?.let { limit ->
            if (nextObjectCount > limit) {
                throw BadRequestException("object quota exceeded for project: $projectId")
            }
        }
        policy.maxTotalBytes?.let { limit ->
            if (nextStoredBytes > limit) {
                throw BadRequestException("storage byte quota exceeded for project: $projectId")
            }
        }
    }

    fun assertRateLimit(projectId: UUID, operation: StorageRateLimitOperation) {
        val policy = policyRepository.findByProjectId(projectId) ?: return
        val limit = when (operation) {
            StorageRateLimitOperation.PRESIGN -> policy.presignPerMinute
            StorageRateLimitOperation.UPLOAD -> policy.uploadPerMinute
            StorageRateLimitOperation.DOWNLOAD -> policy.downloadPerMinute
        } ?: return

        val key = RateLimitKey(projectId, operation)
        val window = Instant.now().epochSecond / 60
        val counter = requestWindows.compute(key) { _, current ->
            when {
                current == null || current.windowEpochMinute != window -> FixedWindowCounter(window, 1)
                else -> FixedWindowCounter(window, current.count + 1)
            }
        } ?: FixedWindowCounter(window, 1)
        if (counter.count > limit) {
            throw TooManyRequestsException("storage rate limit exceeded for project: $projectId")
        }
    }

    @Transactional
    fun recordBucketCreated(tenantId: UUID, projectId: UUID, actorId: String) {
        val projectMeter = getOrCreateProjectMetering(tenantId, projectId, actorId)
        projectMeter.bucketCount += 1
        projectMeter.lastRecordedAt = LocalDateTime.now()
        projectMeteringRepository.save(projectMeter)
    }

    @Transactional
    fun recordBucketDeleted(tenantId: UUID, projectId: UUID, bucketId: UUID, actorId: String) {
        val projectMeter = getOrCreateProjectMetering(tenantId, projectId, actorId)
        projectMeter.bucketCount = (projectMeter.bucketCount - 1).coerceAtLeast(0)
        projectMeter.lastRecordedAt = LocalDateTime.now()
        projectMeteringRepository.save(projectMeter)
        bucketMeteringRepository.findByBucketId(bucketId)?.let(bucketMeteringRepository::delete)
    }

    @Transactional
    fun recordObjectUploaded(tenantId: UUID, projectId: UUID, bucketId: UUID, sizeBytes: Long, actorId: String) {
        val now = LocalDateTime.now()
        val projectMeter = getOrCreateProjectMetering(tenantId, projectId, actorId)
        projectMeter.objectCount += 1
        projectMeter.storedBytes += sizeBytes
        projectMeter.uploadedBytes += sizeBytes
        projectMeter.lastRecordedAt = now
        projectMeteringRepository.save(projectMeter)

        val bucketMeter = getOrCreateBucketMetering(tenantId, projectId, bucketId, actorId)
        bucketMeter.objectCount += 1
        bucketMeter.storedBytes += sizeBytes
        bucketMeter.uploadedBytes += sizeBytes
        bucketMeter.lastRecordedAt = now
        bucketMeteringRepository.save(bucketMeter)
    }

    @Transactional
    fun recordObjectDeleted(tenantId: UUID, projectId: UUID, bucketId: UUID, sizeBytes: Long, actorId: String) {
        val now = LocalDateTime.now()
        val projectMeter = getOrCreateProjectMetering(tenantId, projectId, actorId)
        projectMeter.objectCount = (projectMeter.objectCount - 1).coerceAtLeast(0)
        projectMeter.storedBytes = (projectMeter.storedBytes - sizeBytes).coerceAtLeast(0)
        projectMeter.lastRecordedAt = now
        projectMeteringRepository.save(projectMeter)

        val bucketMeter = getOrCreateBucketMetering(tenantId, projectId, bucketId, actorId)
        bucketMeter.objectCount = (bucketMeter.objectCount - 1).coerceAtLeast(0)
        bucketMeter.storedBytes = (bucketMeter.storedBytes - sizeBytes).coerceAtLeast(0)
        bucketMeter.lastRecordedAt = now
        bucketMeteringRepository.save(bucketMeter)
    }

    @Transactional
    fun recordObjectDownloaded(tenantId: UUID, projectId: UUID, bucketId: UUID, sizeBytes: Long, actorId: String) {
        val now = LocalDateTime.now()
        val projectMeter = getOrCreateProjectMetering(tenantId, projectId, actorId)
        projectMeter.downloadedBytes += sizeBytes
        projectMeter.lastRecordedAt = now
        projectMeteringRepository.save(projectMeter)

        val bucketMeter = getOrCreateBucketMetering(tenantId, projectId, bucketId, actorId)
        bucketMeter.downloadedBytes += sizeBytes
        bucketMeter.lastRecordedAt = now
        bucketMeteringRepository.save(bucketMeter)
    }

    private fun getOrCreateProjectMetering(tenantId: UUID, projectId: UUID, actorId: String): StorageProjectMeteringEntity {
        return projectMeteringRepository.findByProjectId(projectId)
            ?: StorageProjectMeteringEntity(
                tenantId = tenantId,
                projectId = projectId,
                createdBy = actorId
            )
    }

    private fun getOrCreateBucketMetering(
        tenantId: UUID,
        projectId: UUID,
        bucketId: UUID,
        actorId: String
    ): StorageBucketMeteringEntity {
        return bucketMeteringRepository.findByBucketId(bucketId)
            ?: StorageBucketMeteringEntity(
                tenantId = tenantId,
                projectId = projectId,
                bucketId = bucketId,
                createdBy = actorId
            )
    }

    private fun validateProjectScope(tenantId: UUID, projectId: UUID) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ResourceNotFoundException("project not found: $projectId") }
        if (project.tenantId != tenantId) {
            throw BadRequestException("project does not belong to tenant: $projectId")
        }
    }

    private fun requirePositiveOrNull(value: Long?, field: String) {
        if (value != null && value <= 0) {
            throw BadRequestException("$field must be greater than 0")
        }
    }

    private fun normalizeTags(tags: List<String>): List<String> {
        return tags.mapNotNull { raw ->
            raw.trim().lowercase().takeIf { it.isNotBlank() }
        }.distinct()
    }

    private fun StorageProjectMeteringEntity.toSnapshot(): StorageProjectMeteringSnapshot {
        return StorageProjectMeteringSnapshot(
            tenantId = tenantId,
            projectId = projectId,
            bucketCount = bucketCount,
            objectCount = objectCount,
            storedBytes = storedBytes,
            uploadedBytes = uploadedBytes,
            downloadedBytes = downloadedBytes,
            lastRecordedAt = lastRecordedAt
        )
    }

    private fun StorageBucketMeteringEntity.toSnapshot(): StorageBucketMeteringSnapshot {
        return StorageBucketMeteringSnapshot(
            tenantId = tenantId,
            projectId = projectId,
            bucketId = bucketId,
            objectCount = objectCount,
            storedBytes = storedBytes,
            uploadedBytes = uploadedBytes,
            downloadedBytes = downloadedBytes,
            lastRecordedAt = lastRecordedAt
        )
    }

    private data class RateLimitKey(
        val projectId: UUID,
        val operation: StorageRateLimitOperation
    )

    private data class FixedWindowCounter(
        val windowEpochMinute: Long,
        val count: Int
    )
}
