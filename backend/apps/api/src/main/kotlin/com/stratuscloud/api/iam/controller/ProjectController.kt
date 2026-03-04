package com.stratuscloud.api.iam.controller

import com.stratuscloud.api.iam.dto.CreateProjectRequest
import com.stratuscloud.api.iam.dto.ProjectResponse
import com.stratuscloud.iam.service.ProjectService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/projects")
class ProjectController(
    private val projectService: ProjectService
) {

    @PostMapping
    fun createProject(
        @Valid @RequestBody request: CreateProjectRequest,
        @RequestHeader("X-Actor-Id") actorId: UUID
    ): ResponseEntity<ProjectResponse> {
        val created = projectService.createProject(request.tenantId, request.name, actorId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(created))
    }

    @GetMapping("/{projectId}")
    fun getProject(@PathVariable projectId: UUID): ResponseEntity<ProjectResponse> {
        val project = projectService.getProject(projectId)
        return ResponseEntity.ok(ProjectResponse.from(project))
    }
}
