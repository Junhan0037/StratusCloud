package com.stratuscloud.api.iam.dto

import com.stratuscloud.iam.domain.ProjectEntity
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateProjectRequest(
    val tenantId: UUID,
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String
)

data class ProjectResponse(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(project: ProjectEntity): ProjectResponse {
            return ProjectResponse(
                id = project.id ?: error("project id is null"),
                tenantId = project.tenantId,
                name = project.name,
                createdAt = project.createdAt
            )
        }
    }
}
