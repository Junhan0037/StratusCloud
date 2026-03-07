package com.stratuscloud.api.iam.controller

import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.ApiAuditRecorder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.api.iam.dto.CreateProjectRequest
import com.stratuscloud.api.iam.dto.ProjectResponse
import com.stratuscloud.iam.service.IamAction
import com.stratuscloud.iam.service.ProjectService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/projects")
class ProjectController(
    private val projectService: ProjectService,
    private val authorizationFacade: AuthorizationFacade,
    private val apiAuditRecorder: ApiAuditRecorder
) {

    @PostMapping
    fun createProject(
        @Valid @RequestBody request: CreateProjectRequest
    ): ResponseEntity<ProjectResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = null,
            action = IamAction.PROJECT_CREATE,
            resource = "project:*",
            resourceType = "PROJECT",
            resourceId = null,
            metadata = mapOf("name" to request.name)
        )
        val created = projectService.createProject(request.tenantId, request.name, principal.actorId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = created.tenantId,
            projectId = created.id,
            action = IamAction.PROJECT_CREATE,
            resourceType = "PROJECT",
            resourceId = created.id.toString(),
            metadata = mapOf("name" to created.name)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(created))
    }

    @GetMapping("/{projectId}")
    fun getProject(@PathVariable projectId: UUID): ResponseEntity<ProjectResponse> {
        val principal = AuthContextHolder.getRequired()
        val project = projectService.getProject(projectId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = project.tenantId,
            projectId = projectId,
            action = IamAction.PROJECT_READ,
            resource = "project:$projectId",
            resourceType = "PROJECT",
            resourceId = projectId.toString()
        )
        return ResponseEntity.ok(ProjectResponse.from(project))
    }
}
