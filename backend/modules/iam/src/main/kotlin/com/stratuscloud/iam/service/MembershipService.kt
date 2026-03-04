package com.stratuscloud.iam.service

import com.stratuscloud.iam.domain.ProjectMembershipEntity
import com.stratuscloud.iam.domain.RoleType
import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import com.stratuscloud.iam.repository.ProjectMembershipRepository
import com.stratuscloud.iam.repository.ProjectRepository
import com.stratuscloud.iam.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MembershipService(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val membershipRepository: ProjectMembershipRepository
) {

    @Transactional
    fun addMember(projectId: UUID, userId: UUID, role: RoleType, actorId: UUID): ProjectMembershipEntity {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ResourceNotFoundException("project not found: $projectId") }

        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException("user not found: $userId")
        }

        if (membershipRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw DuplicateResourceException("membership already exists")
        }

        return membershipRepository.save(
            ProjectMembershipEntity(
                tenantId = project.tenantId,
                projectId = projectId,
                userId = userId,
                role = role,
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional
    fun changeRole(projectId: UUID, userId: UUID, role: RoleType): ProjectMembershipEntity {
        val membership = membershipRepository.findByProjectIdAndUserId(projectId, userId)
            ?: throw ResourceNotFoundException("membership not found")

        membership.role = role
        return membershipRepository.save(membership)
    }
}
