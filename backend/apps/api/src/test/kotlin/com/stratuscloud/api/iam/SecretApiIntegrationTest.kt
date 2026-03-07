package com.stratuscloud.api.iam

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.stratuscloud.iam.domain.TenantEntity
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
class SecretApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Test
    fun `secret 생성 시 raw 값은 1회만 노출되고 버전 조회에는 포함되지 않아야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "secret-tenant",
                createdBy = "tester"
            )
        )

        val createResponse = mockMvc.post("/v1/iam/secrets") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "name" to "db-password",
                    "value" to "super-secret-value"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("db-password") }
            jsonPath("$.currentVersion.version") { value(1) }
            jsonPath("$.currentVersion.value") { value("super-secret-value") }
        }.andReturn().response.contentAsString

        val secretId = objectMapper.readTree(createResponse).get("id").asText()

        mockMvc.get("/v1/iam/secrets/$secretId/versions") {
            header("X-Project-Role", "ADMIN")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].version") { value(1) }
            jsonPath("$[0].value") { doesNotExist() }
            jsonPath("$[0].status") { value("ACTIVE") }
        }
    }

    @Test
    fun `secret rotate 후 새 버전이 추가되고 기존 버전은 회수할 수 있어야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "rotate-tenant",
                createdBy = "tester"
            )
        )

        val createdBody = mockMvc.post("/v1/iam/secrets") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "name" to "api-token",
                    "value" to "v1-token"
                )
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString

        val createdNode: JsonNode = objectMapper.readTree(createdBody)
        val secretId = createdNode.get("id").asText()
        val firstVersionId = createdNode.path("currentVersion").path("id").asText()

        mockMvc.post("/v1/iam/secrets/$secretId:rotate") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            content = objectMapper.writeValueAsString(
                mapOf(
                    "value" to "v2-token"
                )
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.latestVersion") { value(2) }
            jsonPath("$.currentVersion.version") { value(2) }
            jsonPath("$.currentVersion.value") { value("v2-token") }
        }

        mockMvc.delete("/v1/iam/secrets/$secretId/versions/$firstVersionId") {
            header("X-Project-Role", "ADMIN")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("REVOKED") }
        }

        mockMvc.get("/v1/iam/secrets/$secretId/versions") {
            header("X-Project-Role", "ADMIN")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].version") { value(2) }
            jsonPath("$[0].status") { value("ACTIVE") }
            jsonPath("$[1].version") { value(1) }
            jsonPath("$[1].status") { value("REVOKED") }
        }
    }
}
