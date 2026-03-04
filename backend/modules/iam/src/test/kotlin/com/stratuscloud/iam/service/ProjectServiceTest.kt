package com.stratuscloud.iam.service

import com.stratuscloud.iam.exception.DuplicateResourceException
import com.stratuscloud.iam.repository.ProjectRepository
import com.stratuscloud.iam.repository.TenantRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.assertEquals

class ProjectServiceTest {

    private val tenantRepository: TenantRepository = mock(TenantRepository::class.java)
    private val projectRepository: ProjectRepository = mock(ProjectRepository::class.java)
    private val projectService = ProjectService(tenantRepository, projectRepository)

    @Test
    fun `프로젝트를 생성하면 tenant id와 이름이 보존되어야 한다`() {
        val tenantId = UUID.randomUUID()
        `when`(tenantRepository.existsById(tenantId)).thenReturn(true)
        `when`(projectRepository.existsByTenantIdAndName(tenantId, "project-a")).thenReturn(false)
        `when`(projectRepository.save(any())).thenAnswer { it.arguments.first() }

        val created = projectService.createProject(
            tenantId = tenantId,
            name = "project-a",
            actorId = UUID.randomUUID()
        )

        assertEquals(tenantId, created.tenantId)
        assertEquals("project-a", created.name)
    }

    @Test
    fun `동일 테넌트 내 프로젝트 이름 중복이면 예외가 발생해야 한다`() {
        val tenantId = UUID.randomUUID()
        `when`(tenantRepository.existsById(tenantId)).thenReturn(true)
        `when`(projectRepository.existsByTenantIdAndName(tenantId, "duplicate-project")).thenReturn(true)

        assertThrows<DuplicateResourceException> {
            projectService.createProject(
                tenantId = tenantId,
                name = "duplicate-project",
                actorId = UUID.randomUUID()
            )
        }
    }
}
