package com.stratuscloud.api.iam

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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserMembershipApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    fun `사용자 생성 후 프로젝트 멤버 추가와 역할 변경이 가능해야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "tenant-membership",
                createdBy = "tester"
            )
        )
        val project = projectRepository.save(
            ProjectEntity(
                tenantId = requireNotNull(tenant.id),
                name = "project-membership",
                createdBy = "tester"
            )
        )
        val projectId = requireNotNull(project.id)

        val userCreateResponse = mockMvc.post("/v1/iam/users") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Actor-Id", UUID.randomUUID().toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "email" to "week2-user@example.com",
                    "displayName" to "Week2 User"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.email") { value("week2-user@example.com") }
        }.andReturn().response.contentAsString

        val userId = objectMapper.readTree(userCreateResponse).get("id").asText()

        mockMvc.post("/v1/projects/$projectId/members") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Actor-Id", UUID.randomUUID().toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "userId" to userId,
                    "role" to "DEVELOPER"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.role") { value("DEVELOPER") }
        }

        val roleUpdateResponse = mockMvc.patch("/v1/projects/$projectId/members/$userId/role") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Actor-Id", UUID.randomUUID().toString())
            content = objectMapper.writeValueAsString(mapOf("role" to "ADMIN"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.role") { value("ADMIN") }
        }.andReturn().response.contentAsString

        val roleNode: JsonNode = objectMapper.readTree(roleUpdateResponse)
        assertEquals("ADMIN", roleNode.get("role").asText())
    }
}
