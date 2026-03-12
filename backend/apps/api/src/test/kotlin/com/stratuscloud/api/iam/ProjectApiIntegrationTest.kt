package com.stratuscloud.api.iam

import com.fasterxml.jackson.databind.ObjectMapper
import com.stratuscloud.audit.domain.AuditResult
import com.stratuscloud.audit.repository.AuditEventRepository
import com.stratuscloud.iam.domain.ProjectEntity
import com.stratuscloud.iam.domain.TenantEntity
import com.stratuscloud.iam.repository.ProjectRepository
import com.stratuscloud.iam.repository.TenantRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var auditEventRepository: AuditEventRepository

    @Test
    fun `ADMIN 역할이면 프로젝트 생성이 성공해야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "integration-tenant",
                createdBy = "tester"
            )
        )

        val payload = mapOf(
            "tenantId" to tenant.id,
            "name" to "project-week2"
        )

        mockMvc.post("/v1/projects") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Actor-Id", UUID.randomUUID().toString())
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.tenantId") { value(tenant.id.toString()) }
            jsonPath("$.name") { value("project-week2") }
        }
    }

    @Test
    fun `VIEWER 역할이면 프로젝트 생성이 403이어야 한다`() {
        val payload = mapOf(
            "tenantId" to UUID.randomUUID(),
            "name" to "forbidden-project"
        )

        mockMvc.post("/v1/projects") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "VIEWER")
            header("X-Actor-Id", UUID.randomUUID().toString())
            content = objectMapper.writeValueAsString(payload)
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
            jsonPath("$.traceId") { exists() }
        }
    }

    @Test
    fun `프로젝트 ID로 조회하면 프로젝트 정보를 반환해야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "lookup-tenant",
                createdBy = "tester"
            )
        )
        val project = projectRepository.save(
            ProjectEntity(
                tenantId = requireNotNull(tenant.id),
                name = "lookup-project",
                createdBy = "tester"
            )
        )

        mockMvc.get("/v1/projects/${requireNotNull(project.id)}") {
            header("X-Project-Role", "VIEWER")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(project.id.toString()) }
            jsonPath("$.name") { value("lookup-project") }
        }
    }

    @Test
    fun `다른 tenant 사용자는 프로젝트를 조회할 수 없고 denied audit가 남아야 한다`() {
        val ownerTenant = tenantRepository.save(
            TenantEntity(
                name = "owner-tenant",
                createdBy = "tester"
            )
        )
        val attackerTenant = tenantRepository.save(
            TenantEntity(
                name = "attacker-tenant",
                createdBy = "tester"
            )
        )
        val project = projectRepository.save(
            ProjectEntity(
                tenantId = requireNotNull(ownerTenant.id),
                name = "tenant-isolated-project",
                createdBy = "tester"
            )
        )

        mockMvc.get("/v1/projects/${requireNotNull(project.id)}") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", attackerTenant.id.toString())
            header("X-Actor-Id", UUID.randomUUID().toString())
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }

        val deniedLogs = auditEventRepository.search(
            tenantId = ownerTenant.id,
            projectId = project.id,
            actorId = null,
            resourceType = "PROJECT",
            action = "iam:project:read",
            result = AuditResult.DENIED,
            occurredFrom = null,
            occurredTo = null
        )
        assertThat(deniedLogs).isNotEmpty
    }
}
