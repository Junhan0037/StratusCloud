package com.stratuscloud.api.compute.controller

import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.ApiAuditRecorder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.api.compute.dto.ComputeImageResponse
import com.stratuscloud.api.compute.dto.CreateComputeImageRequest
import com.stratuscloud.compute.domain.ComputeImageStatus
import com.stratuscloud.compute.domain.ComputeOsType
import com.stratuscloud.compute.service.ComputeImageService
import com.stratuscloud.iam.service.IamAction
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/compute/images")
class ComputeImageController(
    private val computeImageService: ComputeImageService,
    private val authorizationFacade: AuthorizationFacade,
    private val apiAuditRecorder: ApiAuditRecorder
) {

    @PostMapping
    fun createImage(
        @Valid @RequestBody request: CreateComputeImageRequest
    ): ResponseEntity<ComputeImageResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = null,
            action = IamAction.COMPUTE_IMAGE_CREATE,
            resource = "tenant:${request.tenantId}",
            resourceType = "COMPUTE_IMAGE",
            resourceId = null,
            metadata = mapOf("name" to request.name, "version" to request.version)
        )
        val created = computeImageService.createImage(
            tenantId = request.tenantId,
            name = request.name,
            version = request.version,
            osType = request.osType,
            status = request.status ?: ComputeImageStatus.ACTIVE,
            tags = request.tags,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = created.tenantId,
            projectId = null,
            action = IamAction.COMPUTE_IMAGE_CREATE,
            resourceType = "COMPUTE_IMAGE",
            resourceId = created.id.toString(),
            metadata = mapOf("name" to created.name, "version" to created.version)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ComputeImageResponse.from(created))
    }

    @GetMapping
    fun listImages(
        @RequestParam tenantId: UUID,
        @RequestParam(required = false) status: ComputeImageStatus?,
        @RequestParam(required = false) osType: ComputeOsType?,
        @RequestParam(required = false) tag: String?
    ): ResponseEntity<List<ComputeImageResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = null,
            action = IamAction.COMPUTE_IMAGE_LIST,
            resource = "tenant:$tenantId",
            resourceType = "COMPUTE_IMAGE",
            resourceId = null
        )
        val images = computeImageService.listImages(tenantId, status, osType, tag).map { ComputeImageResponse.from(it) }
        return ResponseEntity.ok(images)
    }

    @GetMapping("/{imageId}")
    fun getImage(@PathVariable imageId: UUID): ResponseEntity<ComputeImageResponse> {
        val principal = AuthContextHolder.getRequired()
        val image = computeImageService.getImage(imageId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = image.tenantId,
            projectId = null,
            action = IamAction.COMPUTE_IMAGE_READ,
            resource = "compute-image:$imageId",
            resourceType = "COMPUTE_IMAGE",
            resourceId = imageId.toString()
        )
        return ResponseEntity.ok(ComputeImageResponse.from(image))
    }
}
