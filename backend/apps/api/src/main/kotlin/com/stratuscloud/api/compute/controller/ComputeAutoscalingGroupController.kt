package com.stratuscloud.api.compute.controller

import com.stratuscloud.api.common.security.ApiAuditRecorder
import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.api.compute.dto.ComputeAutoscalingGroupResponse
import com.stratuscloud.api.compute.dto.ComputeHealthReconcileResponse
import com.stratuscloud.api.compute.dto.CreateComputeAutoscalingGroupRequest
import com.stratuscloud.compute.service.ComputeAutoscalingGroupService
import com.stratuscloud.compute.service.ComputeHealthcheckService
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
@RequestMapping("/v1/compute/autoscaling-groups")
class ComputeAutoscalingGroupController(
    private val computeAutoscalingGroupService: ComputeAutoscalingGroupService,
    private val computeHealthcheckService: ComputeHealthcheckService,
    private val authorizationFacade: AuthorizationFacade,
    private val apiAuditRecorder: ApiAuditRecorder
) {

    @PostMapping
    fun createGroup(
        @Valid @RequestBody request: CreateComputeAutoscalingGroupRequest
    ): ResponseEntity<ComputeAutoscalingGroupResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.COMPUTE_AUTOSCALING_GROUP_CREATE,
            resource = "project:${request.projectId}",
            resourceType = "COMPUTE_AUTOSCALING_GROUP",
            resourceId = null,
            metadata = mapOf("name" to request.name, "imageId" to request.imageId)
        )
        val created = computeAutoscalingGroupService.createGroup(
            tenantId = request.tenantId,
            projectId = request.projectId,
            name = request.name,
            imageId = request.imageId,
            flavor = request.flavor,
            minInstances = request.minInstances,
            maxInstances = request.maxInstances,
            cpuScaleOutPercent = request.cpuScaleOutPercent,
            cpuScaleInPercent = request.cpuScaleInPercent,
            memoryScaleOutPercent = request.memoryScaleOutPercent,
            memoryScaleInPercent = request.memoryScaleInPercent,
            healthPolicy = request.healthPolicy ?: com.stratuscloud.compute.domain.ComputeHealthPolicy.RESTART,
            failureThreshold = request.failureThreshold ?: 3,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = created.group.tenantId,
            projectId = created.group.projectId,
            action = IamAction.COMPUTE_AUTOSCALING_GROUP_CREATE,
            resourceType = "COMPUTE_AUTOSCALING_GROUP",
            resourceId = created.group.id.toString(),
            metadata = mapOf("name" to created.group.name, "desiredInstances" to created.group.desiredInstances)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ComputeAutoscalingGroupResponse.from(created))
    }

    @GetMapping
    fun listGroups(
        @RequestParam tenantId: UUID,
        @RequestParam projectId: UUID
    ): ResponseEntity<List<ComputeAutoscalingGroupResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.COMPUTE_AUTOSCALING_GROUP_LIST,
            resource = "project:$projectId",
            resourceType = "COMPUTE_AUTOSCALING_GROUP",
            resourceId = null
        )
        return ResponseEntity.ok(
            computeAutoscalingGroupService.listGroups(tenantId, projectId).map { ComputeAutoscalingGroupResponse.from(it) }
        )
    }

    @GetMapping("/{groupId}")
    fun getGroup(@PathVariable groupId: UUID): ResponseEntity<ComputeAutoscalingGroupResponse> {
        val principal = AuthContextHolder.getRequired()
        val group = computeAutoscalingGroupService.getGroup(groupId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = group.tenantId,
            projectId = group.projectId,
            action = IamAction.COMPUTE_AUTOSCALING_GROUP_READ,
            resource = "compute-autoscaling-group:$groupId",
            resourceType = "COMPUTE_AUTOSCALING_GROUP",
            resourceId = groupId.toString()
        )
        return ResponseEntity.ok(ComputeAutoscalingGroupResponse.from(computeAutoscalingGroupService.describe(groupId)))
    }

    @PostMapping("/{groupId}:evaluate")
    fun evaluate(@PathVariable groupId: UUID): ResponseEntity<ComputeAutoscalingGroupResponse> {
        val principal = AuthContextHolder.getRequired()
        val group = computeAutoscalingGroupService.getGroup(groupId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = group.tenantId,
            projectId = group.projectId,
            action = IamAction.COMPUTE_AUTOSCALING_GROUP_EVALUATE,
            resource = "compute-autoscaling-group:$groupId",
            resourceType = "COMPUTE_AUTOSCALING_GROUP",
            resourceId = groupId.toString()
        )
        val evaluated = computeAutoscalingGroupService.evaluate(groupId, principal.actorId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = group.tenantId,
            projectId = group.projectId,
            action = IamAction.COMPUTE_AUTOSCALING_GROUP_EVALUATE,
            resourceType = "COMPUTE_AUTOSCALING_GROUP",
            resourceId = groupId.toString(),
            metadata = mapOf("desiredInstances" to evaluated.group.desiredInstances, "currentInstances" to evaluated.currentInstances)
        )
        return ResponseEntity.ok(ComputeAutoscalingGroupResponse.from(evaluated))
    }

    @PostMapping("/{groupId}:reconcile-health")
    fun reconcileHealth(@PathVariable groupId: UUID): ResponseEntity<ComputeHealthReconcileResponse> {
        val principal = AuthContextHolder.getRequired()
        val group = computeAutoscalingGroupService.getGroup(groupId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = group.tenantId,
            projectId = group.projectId,
            action = IamAction.COMPUTE_AUTOSCALING_GROUP_RECONCILE,
            resource = "compute-autoscaling-group:$groupId",
            resourceType = "COMPUTE_AUTOSCALING_GROUP",
            resourceId = groupId.toString()
        )
        val reconciled = computeHealthcheckService.reconcileGroup(groupId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = group.tenantId,
            projectId = group.projectId,
            action = IamAction.COMPUTE_AUTOSCALING_GROUP_RECONCILE,
            resourceType = "COMPUTE_AUTOSCALING_GROUP",
            resourceId = groupId.toString(),
            metadata = mapOf("restartedCount" to reconciled.restartedInstanceIds.size)
        )
        return ResponseEntity.ok(ComputeHealthReconcileResponse.from(reconciled))
    }
}
