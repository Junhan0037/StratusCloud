package com.stratuscloud.data.service

import com.stratuscloud.data.domain.ManagedDatabaseEngine
import com.stratuscloud.data.domain.ManagedDatabaseEntity
import com.stratuscloud.data.domain.ManagedDatabaseStatus
import com.stratuscloud.data.repository.ManagedDatabaseRepository
import com.stratuscloud.iam.exception.BadRequestException
import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import com.stratuscloud.iam.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ManagedDatabaseService(
    private val projectRepository: ProjectRepository,
    private val managedDatabaseRepository: ManagedDatabaseRepository
) {

    @Transactional
    fun createDatabase(
        tenantId: UUID,
        projectId: UUID,
        name: String,
        engineVersion: String,
        instanceClass: String,
        storageGb: Int,
        actorId: UUID
    ): ManagedDatabaseEntity {
        validateProjectScope(tenantId, projectId)
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            throw BadRequestException("database name must not be blank")
        }
        val normalizedVersion = engineVersion.trim()
        if (normalizedVersion.isBlank()) {
            throw BadRequestException("engine version must not be blank")
        }
        val normalizedInstanceClass = instanceClass.trim()
        if (normalizedInstanceClass.isBlank()) {
            throw BadRequestException("instance class must not be blank")
        }
        if (storageGb !in 10..1024) {
            throw BadRequestException("storageGb must be between 10 and 1024")
        }
        if (managedDatabaseRepository.existsByProjectIdAndName(projectId, normalizedName)) {
            throw DuplicateResourceException("managed database already exists: $normalizedName")
        }
        return managedDatabaseRepository.save(
            ManagedDatabaseEntity(
                tenantId = tenantId,
                projectId = projectId,
                name = normalizedName,
                engine = ManagedDatabaseEngine.POSTGRESQL,
                engineVersion = normalizedVersion,
                instanceClass = normalizedInstanceClass,
                storageGb = storageGb,
                status = ManagedDatabaseStatus.AVAILABLE,
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional(readOnly = true)
    fun listDatabases(tenantId: UUID, projectId: UUID): List<ManagedDatabaseEntity> {
        validateProjectScope(tenantId, projectId)
        return managedDatabaseRepository.findAllByTenantIdAndProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId, projectId)
    }

    @Transactional(readOnly = true)
    fun getDatabase(databaseId: UUID): ManagedDatabaseEntity {
        return managedDatabaseRepository.findById(databaseId)
            .orElseThrow { ResourceNotFoundException("managed database not found: $databaseId") }
    }

    @Transactional
    fun deleteDatabase(databaseId: UUID): ManagedDatabaseEntity {
        val entity = getDatabase(databaseId)
        if (entity.status == ManagedDatabaseStatus.DELETED) {
            return entity
        }
        entity.status = ManagedDatabaseStatus.DELETED
        entity.deletedAt = LocalDateTime.now()
        return managedDatabaseRepository.save(entity)
    }

    private fun validateProjectScope(tenantId: UUID, projectId: UUID) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { ResourceNotFoundException("project not found: $projectId") }
        if (project.tenantId != tenantId) {
            throw BadRequestException("project does not belong to tenant: $projectId")
        }
    }
}
