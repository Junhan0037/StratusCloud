package com.stratuscloud.api.storage.controller

import com.stratuscloud.api.common.security.ApiAuditRecorder
import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.api.storage.dto.StorageBucketMeteringResponse
import com.stratuscloud.api.storage.dto.StorageGovernancePolicyResponse
import com.stratuscloud.api.storage.dto.StorageProjectMeteringResponse
import com.stratuscloud.api.storage.dto.UpdateStorageGovernancePolicyRequest
import com.stratuscloud.governance.service.StorageGovernanceService
import com.stratuscloud.iam.service.IamAction
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/governance/storage")
class StorageGovernanceController(
    private val storageGovernanceService: StorageGovernanceService,
    private val storageService: com.stratuscloud.storage.service.StorageService,
    private val authorizationFacade: AuthorizationFacade,
    private val apiAuditRecorder: ApiAuditRecorder
) {

    @PutMapping("/policies/projects/{projectId}")
    fun upsertPolicy(
        @PathVariable projectId: UUID,
        @Valid @RequestBody request: UpdateStorageGovernancePolicyRequest
    ): ResponseEntity<StorageGovernancePolicyResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = projectId,
            action = IamAction.GOVERNANCE_STORAGE_POLICY_WRITE,
            resource = "project:$projectId",
            resourceType = "GOVERNANCE",
            resourceId = projectId.toString()
        )
        val entity = storageGovernanceService.upsertPolicy(
            tenantId = request.tenantId,
            projectId = projectId,
            maxBucketCount = request.maxBucketCount,
            maxObjectCount = request.maxObjectCount,
            maxTotalBytes = request.maxTotalBytes,
            presignPerMinute = request.presignPerMinute,
            uploadPerMinute = request.uploadPerMinute,
            downloadPerMinute = request.downloadPerMinute,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = entity.tenantId,
            projectId = entity.projectId,
            action = IamAction.GOVERNANCE_STORAGE_POLICY_WRITE,
            resourceType = "GOVERNANCE",
            resourceId = entity.projectId.toString()
        )
        return ResponseEntity.ok(
            StorageGovernancePolicyResponse.from(
                storageGovernanceService.getPolicySnapshot(request.tenantId, projectId)
            )
        )
    }

    @GetMapping("/policies/projects/{projectId}")
    fun getPolicy(@PathVariable projectId: UUID): ResponseEntity<StorageGovernancePolicyResponse> {
        val principal = AuthContextHolder.getRequired()
        val projectMetering = storageGovernanceService.getProjectMetering(principal.tenantId ?: error("tenant missing"), projectId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = projectMetering.tenantId,
            projectId = projectId,
            action = IamAction.GOVERNANCE_STORAGE_POLICY_READ,
            resource = "project:$projectId",
            resourceType = "GOVERNANCE",
            resourceId = projectId.toString()
        )
        return ResponseEntity.ok(
            StorageGovernancePolicyResponse.from(
                storageGovernanceService.getPolicySnapshot(projectMetering.tenantId, projectId)
            )
        )
    }

    @GetMapping("/metering/projects/{projectId}")
    fun getProjectMetering(@PathVariable projectId: UUID): ResponseEntity<StorageProjectMeteringResponse> {
        val principal = AuthContextHolder.getRequired()
        val tenantId = principal.tenantId ?: error("tenant missing")
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.GOVERNANCE_STORAGE_METERING_READ,
            resource = "project:$projectId",
            resourceType = "GOVERNANCE",
            resourceId = projectId.toString()
        )
        return ResponseEntity.ok(
            StorageProjectMeteringResponse.from(storageGovernanceService.getProjectMetering(tenantId, projectId))
        )
    }

    @GetMapping("/metering/buckets/{bucketId}")
    fun getBucketMetering(@PathVariable bucketId: UUID): ResponseEntity<StorageBucketMeteringResponse> {
        val principal = AuthContextHolder.getRequired()
        val bucket = storageService.getBucket(bucketId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = bucket.tenantId,
            projectId = bucket.projectId,
            action = IamAction.GOVERNANCE_STORAGE_METERING_READ,
            resource = "storage-bucket:$bucketId",
            resourceType = "GOVERNANCE",
            resourceId = bucketId.toString()
        )
        return ResponseEntity.ok(
            StorageBucketMeteringResponse.from(
                storageGovernanceService.getBucketMetering(bucket.tenantId, bucket.projectId, bucketId)
            )
        )
    }
}
