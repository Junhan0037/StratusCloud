package com.stratuscloud.iam.repository

import com.stratuscloud.iam.domain.ProjectMembershipEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectMembershipRepository : JpaRepository<ProjectMembershipEntity, UUID> {
    fun existsByProjectIdAndUserId(projectId: UUID, userId: UUID): Boolean
    fun findByProjectIdAndUserId(projectId: UUID, userId: UUID): ProjectMembershipEntity?
}
