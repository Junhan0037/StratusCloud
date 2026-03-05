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
class AuthPolicyApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Test
    fun `정책 생성과 역할 바인딩이 가능해야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "policy-tenant",
                createdBy = "tester"
            )
        )

        val policyCreateResponse = mockMvc.post("/v1/iam/policies") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "name" to "iam-policy-admin",
                    "description" to "admin policy",
                    "document" to mapOf(
                        "version" to "2026-03-05",
                        "statements" to listOf(
                            mapOf(
                                "effect" to "ALLOW",
                                "actions" to listOf("iam:policy:list"),
                                "resources" to listOf("tenant:*")
                            )
                        )
                    )
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("iam-policy-admin") }
        }.andReturn().response.contentAsString

        val policyId = objectMapper.readTree(policyCreateResponse).get("id").asText()

        mockMvc.post("/v1/iam/roles") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "role" to "DEVELOPER",
                    "policyIds" to listOf(policyId)
                )
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].role") { value("DEVELOPER") }
        }

        mockMvc.get("/v1/iam/policies") {
            header("X-Project-Role", "VIEWER")
            queryParam("tenantId", tenant.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].name") { value("iam-policy-admin") }
        }
    }

    @Test
    fun `API Key 회수 후 동일 키 인증은 즉시 실패해야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "apikey-tenant",
                createdBy = "tester"
            )
        )

        val issueResponse = mockMvc.post("/v1/iam/api-keys") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "name" to "ci-automation",
                    "role" to "ADMIN"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.rawKey") { exists() }
        }.andReturn().response.contentAsString

        val node: JsonNode = objectMapper.readTree(issueResponse)
        val keyId = node.get("id").asText()
        val rawKey = node.get("rawKey").asText()

        mockMvc.get("/v1/iam/policies") {
            header("X-API-Key", rawKey)
            queryParam("tenantId", tenant.id.toString())
        }.andExpect {
            status { isOk() }
        }

        mockMvc.delete("/v1/iam/api-keys/$keyId") {
            header("X-Project-Role", "ADMIN")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("REVOKED") }
        }

        mockMvc.get("/v1/iam/policies") {
            header("X-API-Key", rawKey)
            queryParam("tenantId", tenant.id.toString())
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }
    }
}
