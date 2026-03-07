package com.stratuscloud.api.compute

import com.fasterxml.jackson.databind.JsonNode
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
class ComputeApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    fun `활성 이미지로 인스턴스를 생성하고 상태를 제어할 수 있어야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "compute-tenant",
                createdBy = "tester"
            )
        )
        val project = projectRepository.save(
            ProjectEntity(
                tenantId = requireNotNull(tenant.id),
                name = "compute-project",
                createdBy = "tester"
            )
        )

        val imageResponse = mockMvc.post("/v1/compute/images") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "name" to "ubuntu-22-04",
                    "version" to "2026.03",
                    "osType" to "LINUX",
                    "tags" to listOf("stable", "lts")
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("ubuntu-22-04") }
            jsonPath("$.status") { value("ACTIVE") }
        }.andReturn().response.contentAsString

        val imageId = objectMapper.readTree(imageResponse).get("id").asText()

        val instanceResponse = mockMvc.post("/v1/compute/instances") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "projectId" to project.id,
                    "name" to "web-01",
                    "imageId" to imageId,
                    "flavor" to "small",
                    "userData" to "#!/bin/bash\necho ready"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("web-01") }
            jsonPath("$.status") { value("RUNNING") }
        }.andReturn().response.contentAsString

        val instanceId = objectMapper.readTree(instanceResponse).get("id").asText()

        mockMvc.get("/v1/compute/instances/$instanceId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.projectId") { value(project.id.toString()) }
            jsonPath("$.imageId") { value(imageId) }
            jsonPath("$.status") { value("RUNNING") }
        }

        mockMvc.post("/v1/compute/instances/$instanceId:stop") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("STOPPED") }
        }

        mockMvc.post("/v1/compute/instances/$instanceId:start") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("RUNNING") }
        }

        mockMvc.delete("/v1/compute/instances/$instanceId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("TERMINATED") }
        }
    }

    @Test
    fun `비활성 이미지나 종료된 인스턴스에 대한 잘못된 제어는 거부되어야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "compute-invalid-tenant",
                createdBy = "tester"
            )
        )
        val project = projectRepository.save(
            ProjectEntity(
                tenantId = requireNotNull(tenant.id),
                name = "compute-invalid-project",
                createdBy = "tester"
            )
        )

        val deprecatedImageBody = mockMvc.post("/v1/compute/images") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "name" to "ubuntu-old",
                    "version" to "2024.01",
                    "osType" to "LINUX",
                    "status" to "DEPRECATED",
                    "tags" to listOf("legacy")
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("DEPRECATED") }
        }.andReturn().response.contentAsString

        val deprecatedImageId = objectMapper.readTree(deprecatedImageBody).get("id").asText()

        mockMvc.post("/v1/compute/instances") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "projectId" to project.id,
                    "name" to "blocked-instance",
                    "imageId" to deprecatedImageId,
                    "flavor" to "small"
                )
            )
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("BAD_REQUEST") }
        }

        val activeImageBody = mockMvc.post("/v1/compute/images") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "name" to "ubuntu-active",
                    "version" to "2026.03",
                    "osType" to "LINUX",
                    "tags" to listOf("stable")
                )
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString

        val activeImageId = objectMapper.readTree(activeImageBody).get("id").asText()

        val instanceBody = mockMvc.post("/v1/compute/instances") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "projectId" to project.id,
                    "name" to "app-01",
                    "imageId" to activeImageId,
                    "flavor" to "nano"
                )
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString

        val instanceId = objectMapper.readTree(instanceBody).get("id").asText()

        mockMvc.delete("/v1/compute/instances/$instanceId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
        }.andExpect {
            status { isOk() }
        }

        mockMvc.post("/v1/compute/instances/$instanceId:start") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("BAD_REQUEST") }
        }
    }

    @Test
    fun `viewer는 인스턴스 생성이 거부되고 audit 로그에 DENIED가 남아야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "compute-audit-tenant",
                createdBy = "tester"
            )
        )
        val project = projectRepository.save(
            ProjectEntity(
                tenantId = requireNotNull(tenant.id),
                name = "compute-audit-project",
                createdBy = "tester"
            )
        )

        val imageBody = mockMvc.post("/v1/compute/images") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "name" to "ubuntu-view",
                    "version" to "2026.03",
                    "osType" to "LINUX",
                    "tags" to listOf("stable")
                )
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString

        val imageId = objectMapper.readTree(imageBody).get("id").asText()

        mockMvc.post("/v1/compute/instances") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "VIEWER")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Project-Id", project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "projectId" to project.id,
                    "name" to "denied-instance",
                    "imageId" to imageId,
                    "flavor" to "small"
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
            queryParam("projectId", project.id.toString())
            queryParam("action", "compute:instance:create")
            queryParam("result", "DENIED")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].resourceType") { value("COMPUTE_INSTANCE") }
            jsonPath("$[0].projectId") { value(project.id.toString()) }
            jsonPath("$[0].result") { value("DENIED") }
        }
    }
}
