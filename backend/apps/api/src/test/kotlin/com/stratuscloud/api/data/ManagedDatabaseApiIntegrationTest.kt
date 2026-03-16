package com.stratuscloud.api.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.stratuscloud.iam.domain.ProjectEntity
import com.stratuscloud.iam.domain.TenantEntity
import com.stratuscloud.iam.repository.ProjectRepository
import com.stratuscloud.iam.repository.TenantRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManagedDatabaseApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    fun `managed database를 생성하고 조회한 뒤 삭제 상태로 전환할 수 있어야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "data-tenant",
                createdBy = "tester"
            )
        )
        val project = projectRepository.save(
            ProjectEntity(
                tenantId = requireNotNull(tenant.id),
                name = "data-project",
                createdBy = "tester"
            )
        )

        val createResponse = mockMvc.post("/v1/data/databases") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "DEVELOPER")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "projectId" to project.id,
                    "name" to "orders-db",
                    "engineVersion" to "16.2",
                    "instanceClass" to "db-small",
                    "storageGb" to 20
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.tenantId") { value(tenant.id.toString()) }
            jsonPath("$.projectId") { value(project.id.toString()) }
            jsonPath("$.name") { value("orders-db") }
            jsonPath("$.engine") { value("POSTGRESQL") }
            jsonPath("$.engineVersion") { value("16.2") }
            jsonPath("$.instanceClass") { value("db-small") }
            jsonPath("$.storageGb") { value(20) }
            jsonPath("$.status") { value("AVAILABLE") }
        }.andReturn().response.contentAsString

        val databaseId = objectMapper.readTree(createResponse).get("id").asText()

        mockMvc.get("/v1/data/databases") {
            header("X-Project-Role", "DEVELOPER")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
            queryParam("tenantId", tenant.id.toString())
            queryParam("projectId", project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(databaseId) }
            jsonPath("$[0].status") { value("AVAILABLE") }
        }

        mockMvc.get("/v1/data/databases/$databaseId") {
            header("X-Project-Role", "DEVELOPER")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(databaseId) }
            jsonPath("$.status") { value("AVAILABLE") }
        }

        mockMvc.delete("/v1/data/databases/$databaseId") {
            header("X-Project-Role", "DEVELOPER")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(databaseId) }
            jsonPath("$.status") { value("DELETED") }
        }

        mockMvc.get("/v1/data/databases") {
            header("X-Project-Role", "DEVELOPER")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
            queryParam("tenantId", tenant.id.toString())
            queryParam("projectId", project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }

        mockMvc.get("/v1/data/databases/$databaseId") {
            header("X-Project-Role", "DEVELOPER")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("DELETED") }
        }
    }

    @Test
    fun `viewer는 managed database 생성이 거부되고 audit 로그에 denied가 남아야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "data-audit-tenant",
                createdBy = "tester"
            )
        )
        val project = projectRepository.save(
            ProjectEntity(
                tenantId = requireNotNull(tenant.id),
                name = "data-audit-project",
                createdBy = "tester"
            )
        )

        mockMvc.post("/v1/data/databases") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "VIEWER")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "projectId" to project.id,
                    "name" to "blocked-db",
                    "engineVersion" to "16.2",
                    "instanceClass" to "db-small",
                    "storageGb" to 20
                )
            )
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }

        mockMvc.get("/v1/audit/logs") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            queryParam("tenantId", tenant.id.toString())
            queryParam("action", "data:database:create")
            queryParam("result", "DENIED")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].tenantId") { value(tenant.id.toString()) }
            jsonPath("$[0].action") { value("data:database:create") }
            jsonPath("$[0].result") { value("DENIED") }
            jsonPath("$[0].resourceType") { value("MANAGED_DATABASE") }
        }
    }

    @Test
    fun `같은 프로젝트에는 동일한 managed database 이름을 다시 만들 수 없고 다른 테넌트로 조회할 수 없어야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "data-duplicate-tenant",
                createdBy = "tester"
            )
        )
        val project = projectRepository.save(
            ProjectEntity(
                tenantId = requireNotNull(tenant.id),
                name = "data-duplicate-project",
                createdBy = "tester"
            )
        )
        val otherTenant = tenantRepository.save(
            TenantEntity(
                name = "other-tenant",
                createdBy = "tester"
            )
        )

        val body = objectMapper.writeValueAsString(
            mapOf(
                "tenantId" to tenant.id,
                "projectId" to project.id,
                "name" to "shared-db",
                "engineVersion" to "16.2",
                "instanceClass" to "db-small",
                "storageGb" to 20
            )
        )

        val created = mockMvc.post("/v1/data/databases") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
            content = body
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString

        val databaseId = objectMapper.readTree(created).get("id").asText()

        mockMvc.post("/v1/data/databases") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
            content = body
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("DUPLICATE_RESOURCE") }
        }

        mockMvc.get("/v1/data/databases/$databaseId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", otherTenant.id.toString())
            header("X-Project-Id", project.id.toString())
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }
    }
}
