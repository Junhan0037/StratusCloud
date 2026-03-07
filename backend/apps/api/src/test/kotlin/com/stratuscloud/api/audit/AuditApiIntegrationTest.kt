package com.stratuscloud.api.audit

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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Test
    fun `권한 거부 요청은 audit 로그에 DENIED 결과로 남아야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "audit-tenant",
                createdBy = "tester"
            )
        )

        mockMvc.post("/v1/projects") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "VIEWER")
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "name" to "should-be-denied"
                )
            )
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }

        mockMvc.get("/v1/audit/logs") {
            header("X-Project-Role", "ADMIN")
            queryParam("tenantId", tenant.id.toString())
            queryParam("action", "iam:project:create")
            queryParam("result", "DENIED")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].tenantId") { value(tenant.id.toString()) }
            jsonPath("$[0].action") { value("iam:project:create") }
            jsonPath("$[0].result") { value("DENIED") }
            jsonPath("$[0].resourceType") { value("PROJECT") }
        }
    }
}
