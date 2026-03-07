package com.stratuscloud.api.compute.controller

import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.ApiAuditRecorder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.api.compute.dto.ComputeInstanceResponse
import com.stratuscloud.api.compute.dto.CreateComputeInstanceRequest
import com.stratuscloud.compute.service.ComputeInstanceService
import com.stratuscloud.iam.service.IamAction
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/compute/instances")
class ComputeInstanceController(
    private val computeInstanceService: ComputeInstanceService,
    private val authorizationFacade: AuthorizationFacade,
    private val apiAuditRecorder: ApiAuditRecorder
) {

    @PostMapping
    fun createInstance(
        @Valid @RequestBody request: CreateComputeInstanceRequest
    ): ResponseEntity<ComputeInstanceResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.COMPUTE_INSTANCE_CREATE,
            resource = "project:${request.projectId}",
            resourceType = "COMPUTE_INSTANCE",
            resourceId = null,
            metadata = mapOf("name" to request.name, "imageId" to request.imageId)
        )
        val created = computeInstanceService.createInstance(
            tenantId = request.tenantId,
            projectId = request.projectId,
            name = request.name,
            imageId = request.imageId,
            flavor = request.flavor,
            userData = request.userData,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = created.tenantId,
            projectId = created.projectId,
            action = IamAction.COMPUTE_INSTANCE_CREATE,
            resourceType = "COMPUTE_INSTANCE",
            resourceId = created.id.toString(),
            metadata = mapOf("name" to created.name, "status" to created.status.name)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ComputeInstanceResponse.from(created))
    }

    @GetMapping
    fun listInstances(
        @RequestParam tenantId: UUID,
        @RequestParam projectId: UUID
    ): ResponseEntity<List<ComputeInstanceResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.COMPUTE_INSTANCE_LIST,
            resource = "project:$projectId",
            resourceType = "COMPUTE_INSTANCE",
            resourceId = null
        )
        val instances = computeInstanceService.listInstances(tenantId, projectId).map { ComputeInstanceResponse.from(it) }
        return ResponseEntity.ok(instances)
    }

    @GetMapping("/{instanceId}")
    fun getInstance(@PathVariable instanceId: UUID): ResponseEntity<ComputeInstanceResponse> {
        val principal = AuthContextHolder.getRequired()
        val instance = computeInstanceService.getInstance(instanceId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = instance.tenantId,
            projectId = instance.projectId,
            action = IamAction.COMPUTE_INSTANCE_READ,
            resource = "compute-instance:$instanceId",
            resourceType = "COMPUTE_INSTANCE",
            resourceId = instanceId.toString()
        )
        return ResponseEntity.ok(ComputeInstanceResponse.from(instance))
    }

    @PostMapping("/{instanceId}:start")
    fun startInstance(@PathVariable instanceId: UUID): ResponseEntity<ComputeInstanceResponse> {
        val principal = AuthContextHolder.getRequired()
        val instance = computeInstanceService.getInstance(instanceId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = instance.tenantId,
            projectId = instance.projectId,
            action = IamAction.COMPUTE_INSTANCE_START,
            resource = "compute-instance:$instanceId",
            resourceType = "COMPUTE_INSTANCE",
            resourceId = instanceId.toString()
        )
        val updated = computeInstanceService.startInstance(instanceId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = updated.tenantId,
            projectId = updated.projectId,
            action = IamAction.COMPUTE_INSTANCE_START,
            resourceType = "COMPUTE_INSTANCE",
            resourceId = updated.id.toString(),
            metadata = mapOf("status" to updated.status.name)
        )
        return ResponseEntity.ok(ComputeInstanceResponse.from(updated))
    }

    @PostMapping("/{instanceId}:stop")
    fun stopInstance(@PathVariable instanceId: UUID): ResponseEntity<ComputeInstanceResponse> {
        val principal = AuthContextHolder.getRequired()
        val instance = computeInstanceService.getInstance(instanceId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = instance.tenantId,
            projectId = instance.projectId,
            action = IamAction.COMPUTE_INSTANCE_STOP,
            resource = "compute-instance:$instanceId",
            resourceType = "COMPUTE_INSTANCE",
            resourceId = instanceId.toString()
        )
        val updated = computeInstanceService.stopInstance(instanceId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = updated.tenantId,
            projectId = updated.projectId,
            action = IamAction.COMPUTE_INSTANCE_STOP,
            resourceType = "COMPUTE_INSTANCE",
            resourceId = updated.id.toString(),
            metadata = mapOf("status" to updated.status.name)
        )
        return ResponseEntity.ok(ComputeInstanceResponse.from(updated))
    }

    @DeleteMapping("/{instanceId}")
    fun terminateInstance(@PathVariable instanceId: UUID): ResponseEntity<ComputeInstanceResponse> {
        val principal = AuthContextHolder.getRequired()
        val instance = computeInstanceService.getInstance(instanceId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = instance.tenantId,
            projectId = instance.projectId,
            action = IamAction.COMPUTE_INSTANCE_TERMINATE,
            resource = "compute-instance:$instanceId",
            resourceType = "COMPUTE_INSTANCE",
            resourceId = instanceId.toString()
        )
        val updated = computeInstanceService.terminateInstance(instanceId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = updated.tenantId,
            projectId = updated.projectId,
            action = IamAction.COMPUTE_INSTANCE_TERMINATE,
            resourceType = "COMPUTE_INSTANCE",
            resourceId = updated.id.toString(),
            metadata = mapOf("status" to updated.status.name)
        )
        return ResponseEntity.ok(ComputeInstanceResponse.from(updated))
    }
}
