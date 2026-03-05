package com.stratuscloud.api.iam.controller

import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.iam.dto.AddMembershipRequest
import com.stratuscloud.api.iam.dto.MembershipResponse
import com.stratuscloud.api.iam.dto.UpdateMembershipRoleRequest
import com.stratuscloud.iam.service.AuthorizationService
import com.stratuscloud.iam.service.IamAction
import com.stratuscloud.iam.service.MembershipService
import com.stratuscloud.iam.service.ProjectService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/projects/{projectId}/members")
class MembershipController(
    private val membershipService: MembershipService,
    private val projectService: ProjectService,
    private val authorizationService: AuthorizationService
) {

    @PostMapping
    fun addMember(
        @PathVariable projectId: UUID,
        @Valid @RequestBody request: AddMembershipRequest
    ): ResponseEntity<MembershipResponse> {
        val principal = AuthContextHolder.getRequired()
        val project = projectService.getProject(projectId)
        authorizationService.authorize(
            principal = principal,
            tenantId = project.tenantId,
            projectId = projectId,
            action = IamAction.MEMBERSHIP_ADD,
            resource = "project:$projectId"
        )
        val created = membershipService.addMember(projectId, request.userId, request.role, principal.actorId)
        return ResponseEntity.status(HttpStatus.CREATED).body(MembershipResponse.from(created))
    }

    @PatchMapping("/{userId}/role")
    fun changeRole(
        @PathVariable projectId: UUID,
        @PathVariable userId: UUID,
        @Valid @RequestBody request: UpdateMembershipRoleRequest
    ): ResponseEntity<MembershipResponse> {
        val principal = AuthContextHolder.getRequired()
        val project = projectService.getProject(projectId)
        authorizationService.authorize(
            principal = principal,
            tenantId = project.tenantId,
            projectId = projectId,
            action = IamAction.MEMBERSHIP_ROLE_UPDATE,
            resource = "project:$projectId"
        )
        val updated = membershipService.changeRole(projectId, userId, request.role)
        return ResponseEntity.ok(MembershipResponse.from(updated))
    }
}
