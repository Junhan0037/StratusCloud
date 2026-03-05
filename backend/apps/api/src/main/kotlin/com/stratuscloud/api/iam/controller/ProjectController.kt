package com.stratuscloud.api.iam.controller

import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.iam.dto.CreateProjectRequest
import com.stratuscloud.api.iam.dto.ProjectResponse
import com.stratuscloud.iam.service.AuthorizationService
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
    private val authorizationService: AuthorizationService
) {

    @PostMapping
    fun createProject(
        @Valid @RequestBody request: CreateProjectRequest
    ): ResponseEntity<ProjectResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationService.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = null,
            action = IamAction.PROJECT_CREATE,
            resource = "project:*"
        )
        val created = projectService.createProject(request.tenantId, request.name, principal.actorId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(created))
    }

    @GetMapping("/{projectId}")
    fun getProject(@PathVariable projectId: UUID): ResponseEntity<ProjectResponse> {
        val principal = AuthContextHolder.getRequired()
        val project = projectService.getProject(projectId)
        authorizationService.authorize(
            principal = principal,
            tenantId = project.tenantId,
            projectId = projectId,
            action = IamAction.PROJECT_READ,
            resource = "project:$projectId"
        )
        return ResponseEntity.ok(ProjectResponse.from(project))
    }
}
