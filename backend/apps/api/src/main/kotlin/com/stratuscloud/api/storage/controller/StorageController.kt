package com.stratuscloud.api.storage.controller

import com.stratuscloud.api.common.security.ApiAuditRecorder
import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.api.storage.dto.StorageTagsResponse
import com.stratuscloud.api.storage.dto.BucketResponse
import com.stratuscloud.api.storage.dto.CreateBucketRequest
import com.stratuscloud.api.storage.dto.CreateObjectPresignRequest
import com.stratuscloud.api.storage.dto.ObjectPresignResponse
import com.stratuscloud.api.storage.dto.StorageObjectResponse
import com.stratuscloud.api.storage.dto.UpdateStorageTagsRequest
import com.stratuscloud.governance.domain.StorageTagResourceType
import com.stratuscloud.governance.service.StorageGovernanceService
import com.stratuscloud.iam.service.IamAction
import com.stratuscloud.storage.domain.StorageObjectAcl
import com.stratuscloud.storage.service.StorageService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/storage")
class StorageController(
    private val storageService: StorageService,
    private val storageGovernanceService: StorageGovernanceService,
    private val authorizationFacade: AuthorizationFacade,
    private val apiAuditRecorder: ApiAuditRecorder
) {

    @PostMapping("/buckets")
    fun createBucket(@Valid @RequestBody request: CreateBucketRequest): ResponseEntity<BucketResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.STORAGE_BUCKET_CREATE,
            resource = "project:${request.projectId}",
            resourceType = "BUCKET",
            resourceId = null,
            metadata = mapOf("name" to request.name)
        )
        val created = storageService.createBucket(
            tenantId = request.tenantId,
            projectId = request.projectId,
            name = request.name,
            acl = request.acl ?: StorageObjectAcl.PRIVATE,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = created.tenantId,
            projectId = created.projectId,
            action = IamAction.STORAGE_BUCKET_CREATE,
            resourceType = "BUCKET",
            resourceId = created.id.toString(),
            metadata = mapOf("name" to created.name)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(BucketResponse.from(created, 0))
    }

    @GetMapping("/buckets")
    fun listBuckets(
        @RequestParam tenantId: UUID,
        @RequestParam projectId: UUID
    ): ResponseEntity<List<BucketResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.STORAGE_BUCKET_LIST,
            resource = "project:$projectId",
            resourceType = "BUCKET",
            resourceId = null
        )
        val buckets = storageService.listBuckets(tenantId, projectId).map { bucket ->
            BucketResponse.from(bucket, storageService.countObjects(requireNotNull(bucket.id)))
        }
        return ResponseEntity.ok(buckets)
    }

    @GetMapping("/buckets/{bucketId}")
    fun getBucket(@PathVariable bucketId: UUID): ResponseEntity<BucketResponse> {
        val principal = AuthContextHolder.getRequired()
        val bucket = storageService.getBucket(bucketId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = bucket.tenantId,
            projectId = bucket.projectId,
            action = IamAction.STORAGE_BUCKET_READ,
            resource = "storage-bucket:$bucketId",
            resourceType = "BUCKET",
            resourceId = bucketId.toString()
        )
        return ResponseEntity.ok(BucketResponse.from(bucket, storageService.countObjects(bucketId)))
    }

    @DeleteMapping("/buckets/{bucketId}")
    fun deleteBucket(@PathVariable bucketId: UUID): ResponseEntity<Void> {
        val principal = AuthContextHolder.getRequired()
        val bucket = storageService.getBucket(bucketId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = bucket.tenantId,
            projectId = bucket.projectId,
            action = IamAction.STORAGE_BUCKET_DELETE,
            resource = "storage-bucket:$bucketId",
            resourceType = "BUCKET",
            resourceId = bucketId.toString()
        )
        storageService.deleteBucket(bucketId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = bucket.tenantId,
            projectId = bucket.projectId,
            action = IamAction.STORAGE_BUCKET_DELETE,
            resourceType = "BUCKET",
            resourceId = bucketId.toString()
        )
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/buckets/{bucketId}/objects")
    fun listObjects(@PathVariable bucketId: UUID): ResponseEntity<List<StorageObjectResponse>> {
        val principal = AuthContextHolder.getRequired()
        val bucket = storageService.getBucket(bucketId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = bucket.tenantId,
            projectId = bucket.projectId,
            action = IamAction.STORAGE_OBJECT_LIST,
            resource = "storage-bucket:$bucketId",
            resourceType = "OBJECT",
            resourceId = null
        )
        return ResponseEntity.ok(storageService.listObjects(bucketId).map(StorageObjectResponse::from))
    }

    @PostMapping("/buckets/{bucketId}/objects:presign")
    fun createPresign(
        @PathVariable bucketId: UUID,
        @Valid @RequestBody request: CreateObjectPresignRequest
    ): ResponseEntity<ObjectPresignResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.STORAGE_OBJECT_PRESIGN,
            resource = "storage-bucket:$bucketId",
            resourceType = "OBJECT",
            resourceId = null,
            metadata = mapOf("operation" to request.operation.name, "key" to request.key)
        )
        val created = storageService.createPresignedToken(
            tenantId = request.tenantId,
            projectId = request.projectId,
            bucketId = bucketId,
            operation = request.operation,
            key = request.key,
            contentType = request.contentType,
            acl = request.acl,
            expiresInSeconds = request.expiresInSeconds,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = created.tenantId,
            projectId = created.projectId,
            action = IamAction.STORAGE_OBJECT_PRESIGN,
            resourceType = "OBJECT",
            resourceId = null,
            metadata = mapOf("operation" to created.operation.name, "key" to created.objectKey)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ObjectPresignResponse.from(created))
    }

    @DeleteMapping("/objects/{objectId}")
    fun deleteObject(@PathVariable objectId: UUID): ResponseEntity<Void> {
        val principal = AuthContextHolder.getRequired()
        val entity = storageService.getObject(objectId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = entity.tenantId,
            projectId = entity.projectId,
            action = IamAction.STORAGE_OBJECT_DELETE,
            resource = "storage-object:$objectId",
            resourceType = "OBJECT",
            resourceId = objectId.toString()
        )
        storageService.deleteObject(objectId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = entity.tenantId,
            projectId = entity.projectId,
            action = IamAction.STORAGE_OBJECT_DELETE,
            resourceType = "OBJECT",
            resourceId = objectId.toString(),
            metadata = mapOf("key" to entity.key)
        )
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/buckets/{bucketId}/tags")
    fun updateBucketTags(
        @PathVariable bucketId: UUID,
        @RequestBody request: UpdateStorageTagsRequest
    ): ResponseEntity<StorageTagsResponse> {
        val principal = AuthContextHolder.getRequired()
        val bucket = storageService.getBucket(bucketId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = bucket.tenantId,
            projectId = bucket.projectId,
            action = IamAction.STORAGE_BUCKET_TAGS_WRITE,
            resource = "storage-bucket:$bucketId",
            resourceType = "BUCKET",
            resourceId = bucketId.toString()
        )
        val tags = storageGovernanceService.replaceTags(
            tenantId = bucket.tenantId,
            projectId = bucket.projectId,
            resourceType = StorageTagResourceType.BUCKET,
            resourceId = bucketId,
            tags = request.tags,
            actorId = principal.actorId
        )
        return ResponseEntity.ok(StorageTagsResponse(resourceId = bucketId, tags = tags))
    }

    @GetMapping("/buckets/{bucketId}/tags")
    fun getBucketTags(@PathVariable bucketId: UUID): ResponseEntity<StorageTagsResponse> {
        val principal = AuthContextHolder.getRequired()
        val bucket = storageService.getBucket(bucketId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = bucket.tenantId,
            projectId = bucket.projectId,
            action = IamAction.STORAGE_BUCKET_TAGS_READ,
            resource = "storage-bucket:$bucketId",
            resourceType = "BUCKET",
            resourceId = bucketId.toString()
        )
        return ResponseEntity.ok(
            StorageTagsResponse(
                resourceId = bucketId,
                tags = storageGovernanceService.listTags(
                    tenantId = bucket.tenantId,
                    projectId = bucket.projectId,
                    resourceType = StorageTagResourceType.BUCKET,
                    resourceId = bucketId
                )
            )
        )
    }

    @PutMapping("/objects/{objectId}/tags")
    fun updateObjectTags(
        @PathVariable objectId: UUID,
        @RequestBody request: UpdateStorageTagsRequest
    ): ResponseEntity<StorageTagsResponse> {
        val principal = AuthContextHolder.getRequired()
        val entity = storageService.getObject(objectId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = entity.tenantId,
            projectId = entity.projectId,
            action = IamAction.STORAGE_OBJECT_TAGS_WRITE,
            resource = "storage-object:$objectId",
            resourceType = "OBJECT",
            resourceId = objectId.toString()
        )
        val tags = storageGovernanceService.replaceTags(
            tenantId = entity.tenantId,
            projectId = entity.projectId,
            resourceType = StorageTagResourceType.OBJECT,
            resourceId = objectId,
            tags = request.tags,
            actorId = principal.actorId
        )
        return ResponseEntity.ok(StorageTagsResponse(resourceId = objectId, tags = tags))
    }

    @GetMapping("/objects/{objectId}/tags")
    fun getObjectTags(@PathVariable objectId: UUID): ResponseEntity<StorageTagsResponse> {
        val principal = AuthContextHolder.getRequired()
        val entity = storageService.getObject(objectId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = entity.tenantId,
            projectId = entity.projectId,
            action = IamAction.STORAGE_OBJECT_TAGS_READ,
            resource = "storage-object:$objectId",
            resourceType = "OBJECT",
            resourceId = objectId.toString()
        )
        return ResponseEntity.ok(
            StorageTagsResponse(
                resourceId = objectId,
                tags = storageGovernanceService.listTags(
                    tenantId = entity.tenantId,
                    projectId = entity.projectId,
                    resourceType = StorageTagResourceType.OBJECT,
                    resourceId = objectId
                )
            )
        )
    }
}
