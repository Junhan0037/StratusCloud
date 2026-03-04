package com.stratuscloud.api.iam.controller

import com.stratuscloud.api.iam.dto.AddMembershipRequest
import com.stratuscloud.api.iam.dto.MembershipResponse
import com.stratuscloud.api.iam.dto.UpdateMembershipRoleRequest
import com.stratuscloud.iam.service.MembershipService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/projects/{projectId}/members")
class MembershipController(
    private val membershipService: MembershipService
) {

    @PostMapping
    fun addMember(
        @PathVariable projectId: UUID,
        @Valid @RequestBody request: AddMembershipRequest,
        @RequestHeader("X-Actor-Id") actorId: UUID
    ): ResponseEntity<MembershipResponse> {
        val created = membershipService.addMember(projectId, request.userId, request.role, actorId)
        return ResponseEntity.status(HttpStatus.CREATED).body(MembershipResponse.from(created))
    }

    @PatchMapping("/{userId}/role")
    fun changeRole(
        @PathVariable projectId: UUID,
        @PathVariable userId: UUID,
        @Valid @RequestBody request: UpdateMembershipRoleRequest
    ): ResponseEntity<MembershipResponse> {
        val updated = membershipService.changeRole(projectId, userId, request.role)
        return ResponseEntity.ok(MembershipResponse.from(updated))
    }
}
