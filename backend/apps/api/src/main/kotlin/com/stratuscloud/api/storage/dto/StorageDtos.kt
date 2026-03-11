package com.stratuscloud.api.storage.dto

import com.stratuscloud.governance.service.StorageBucketMeteringSnapshot
import com.stratuscloud.governance.service.StoragePolicySnapshot
import com.stratuscloud.governance.service.StorageProjectMeteringSnapshot
import com.stratuscloud.storage.domain.StorageBucketEntity
import com.stratuscloud.storage.domain.StorageObjectAcl
import com.stratuscloud.storage.domain.StorageObjectEntity
import com.stratuscloud.storage.domain.StoragePresignOperation
import com.stratuscloud.storage.domain.StoragePresignedTokenEntity
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateBucketRequest(
    val tenantId: UUID,
    val projectId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    val acl: StorageObjectAcl? = null
)

data class BucketResponse(
    val id: UUID,
    val tenantId: UUID,
    val projectId: UUID,
    val name: String,
    val acl: StorageObjectAcl,
    val objectCount: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: StorageBucketEntity, objectCount: Long): BucketResponse {
            return BucketResponse(
                id = entity.id ?: error("bucket id is null"),
                tenantId = entity.tenantId,
                projectId = entity.projectId,
                name = entity.name,
                acl = entity.acl,
                objectCount = objectCount,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class CreateObjectPresignRequest(
    val tenantId: UUID,
    val projectId: UUID,
    val operation: StoragePresignOperation,
    @field:NotBlank
    @field:Size(min = 1, max = 300)
    val key: String,
    @field:Size(max = 120)
    val contentType: String? = null,
    val acl: StorageObjectAcl? = null,
    @field:Min(0)
    @field:Max(3600)
    val expiresInSeconds: Int = 900
)

data class ObjectPresignResponse(
    val token: UUID,
    val operation: StoragePresignOperation,
    val url: String,
    val expiresAt: LocalDateTime
) {
    companion object {
        fun from(entity: StoragePresignedTokenEntity): ObjectPresignResponse {
            val token = entity.id
            val url = when (entity.operation) {
                StoragePresignOperation.UPLOAD -> "/v1/storage/presigned/upload/$token"
                StoragePresignOperation.DOWNLOAD -> "/v1/storage/presigned/download/$token"
            }
            return ObjectPresignResponse(
                token = token,
                operation = entity.operation,
                url = url,
                expiresAt = entity.expiresAt
            )
        }
    }
}

data class StorageObjectResponse(
    val id: UUID,
    val bucketId: UUID,
    val key: String,
    val contentType: String,
    val sizeBytes: Long,
    val etag: String,
    val acl: StorageObjectAcl,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: StorageObjectEntity): StorageObjectResponse {
            return StorageObjectResponse(
                id = entity.id ?: error("object id is null"),
                bucketId = entity.bucketId,
                key = entity.key,
                contentType = entity.contentType,
                sizeBytes = entity.sizeBytes,
                etag = entity.etag,
                acl = entity.acl,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class UpdateStorageTagsRequest(
    val tags: List<String> = emptyList()
)

data class StorageTagsResponse(
    val resourceId: UUID,
    val tags: List<String>
)

data class UpdateStorageGovernancePolicyRequest(
    val tenantId: UUID,
    val maxBucketCount: Int? = null,
    val maxObjectCount: Long? = null,
    val maxTotalBytes: Long? = null,
    val presignPerMinute: Int? = null,
    val uploadPerMinute: Int? = null,
    val downloadPerMinute: Int? = null
)

data class StorageGovernancePolicyResponse(
    val tenantId: UUID,
    val projectId: UUID,
    val maxBucketCount: Int?,
    val maxObjectCount: Long?,
    val maxTotalBytes: Long?,
    val presignPerMinute: Int?,
    val uploadPerMinute: Int?,
    val downloadPerMinute: Int?
) {
    companion object {
        fun from(snapshot: StoragePolicySnapshot): StorageGovernancePolicyResponse {
            return StorageGovernancePolicyResponse(
                tenantId = snapshot.tenantId,
                projectId = snapshot.projectId,
                maxBucketCount = snapshot.maxBucketCount,
                maxObjectCount = snapshot.maxObjectCount,
                maxTotalBytes = snapshot.maxTotalBytes,
                presignPerMinute = snapshot.presignPerMinute,
                uploadPerMinute = snapshot.uploadPerMinute,
                downloadPerMinute = snapshot.downloadPerMinute
            )
        }
    }
}

data class StorageProjectMeteringResponse(
    val tenantId: UUID,
    val projectId: UUID,
    val bucketCount: Long,
    val objectCount: Long,
    val storedBytes: Long,
    val uploadedBytes: Long,
    val downloadedBytes: Long,
    val lastRecordedAt: LocalDateTime
) {
    companion object {
        fun from(snapshot: StorageProjectMeteringSnapshot): StorageProjectMeteringResponse {
            return StorageProjectMeteringResponse(
                tenantId = snapshot.tenantId,
                projectId = snapshot.projectId,
                bucketCount = snapshot.bucketCount,
                objectCount = snapshot.objectCount,
                storedBytes = snapshot.storedBytes,
                uploadedBytes = snapshot.uploadedBytes,
                downloadedBytes = snapshot.downloadedBytes,
                lastRecordedAt = snapshot.lastRecordedAt
            )
        }
    }
}

data class StorageBucketMeteringResponse(
    val tenantId: UUID,
    val projectId: UUID,
    val bucketId: UUID,
    val objectCount: Long,
    val storedBytes: Long,
    val uploadedBytes: Long,
    val downloadedBytes: Long,
    val lastRecordedAt: LocalDateTime
) {
    companion object {
        fun from(snapshot: StorageBucketMeteringSnapshot): StorageBucketMeteringResponse {
            return StorageBucketMeteringResponse(
                tenantId = snapshot.tenantId,
                projectId = snapshot.projectId,
                bucketId = snapshot.bucketId,
                objectCount = snapshot.objectCount,
                storedBytes = snapshot.storedBytes,
                uploadedBytes = snapshot.uploadedBytes,
                downloadedBytes = snapshot.downloadedBytes,
                lastRecordedAt = snapshot.lastRecordedAt
            )
        }
    }
}
