package com.stratuscloud.compute.service

import com.stratuscloud.compute.domain.ComputeImageEntity
import com.stratuscloud.compute.domain.ComputeImageStatus
import com.stratuscloud.compute.domain.ComputeOsType
import com.stratuscloud.compute.repository.ComputeImageRepository
import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ComputeImageService(
    private val computeImageRepository: ComputeImageRepository
) {

    @Transactional
    fun createImage(
        tenantId: UUID,
        name: String,
        version: String,
        osType: ComputeOsType,
        status: ComputeImageStatus,
        tags: List<String>,
        actorId: UUID
    ): ComputeImageEntity {
        val normalizedName = name.trim()
        val normalizedVersion = version.trim()
        if (computeImageRepository.existsByTenantIdAndNameAndVersion(tenantId, normalizedName, normalizedVersion)) {
            throw DuplicateResourceException("compute image already exists: $normalizedName:$normalizedVersion")
        }
        return computeImageRepository.save(
            ComputeImageEntity(
                tenantId = tenantId,
                name = normalizedName,
                version = normalizedVersion,
                osType = osType,
                status = status,
                tags = normalizeTags(tags).joinToString(","),
                createdBy = actorId.toString()
            )
        )
    }

    @Transactional(readOnly = true)
    fun listImages(
        tenantId: UUID,
        status: ComputeImageStatus?,
        osType: ComputeOsType?,
        tag: String?
    ): List<ComputeImageEntity> {
        val normalizedTag = tag?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        return computeImageRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
            .asSequence()
            .filter { status == null || it.status == status }
            .filter { osType == null || it.osType == osType }
            .filter { normalizedTag == null || it.tagSet().contains(normalizedTag) }
            .toList()
    }

    @Transactional(readOnly = true)
    fun getImage(imageId: UUID): ComputeImageEntity {
        return computeImageRepository.findById(imageId)
            .orElseThrow { ResourceNotFoundException("compute image not found: $imageId") }
    }

    fun ComputeImageEntity.tagSet(): Set<String> {
        return tags.split(",")
            .mapNotNull { value -> value.trim().lowercase().takeIf { it.isNotBlank() } }
            .toSet()
    }

    private fun normalizeTags(tags: List<String>): List<String> {
        return tags.mapNotNull { value ->
            value.trim().lowercase().takeIf { it.isNotBlank() }
        }.distinct()
    }
}
