package com.stratuscloud.iam.service

import com.stratuscloud.iam.domain.ProjectEntity
import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import com.stratuscloud.iam.repository.ProjectRepository
import com.stratuscloud.iam.repository.TenantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProjectService(
    private val tenantRepository: TenantRepository,
    private val projectRepository: ProjectRepository
) {

    @Transactional
    fun createProject(tenantId: UUID, name: String, actorId: UUID): ProjectEntity {
        val normalizedName = name.trim()
        if (!tenantRepository.existsById(tenantId)) {
            throw ResourceNotFoundException("tenant not found: $tenantId")
        }
        if (projectRepository.existsByTenantIdAndName(tenantId, normalizedName)) {
            throw DuplicateResourceException("project name already exists in tenant")
        }

        return projectRepository.save(
            ProjectEntity(
                tenantId = tenantId,
                name = normalizedName,
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional(readOnly = true)
    fun getProject(projectId: UUID): ProjectEntity {
        return projectRepository.findById(projectId)
            .orElseThrow { ResourceNotFoundException("project not found: $projectId") }
    }
}
