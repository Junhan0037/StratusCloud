package com.stratuscloud.api.system

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
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperationsApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Test
    fun `actuator health는 인증 없이 조회할 수 있어야 한다`() {
        mockMvc.get("/actuator/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("UP") }
            }
    }

    @Test
    fun `운영 요약 API는 인증이 없으면 401이어야 한다`() {
        mockMvc.get("/v1/system/operations/summary")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("UNAUTHORIZED") }
            }
    }

    @Test
    fun `viewer는 운영 메트릭을 조회할 수 없어야 한다`() {
        mockMvc.get("/v1/system/operations/http-metrics") {
            header("X-Project-Role", "VIEWER")
            header("X-Tenant-Id", UUID.randomUUID().toString())
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }
    }

    @Test
    fun `admin은 운영 요약과 원시 메트릭을 조회할 수 있어야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "ops-summary-tenant",
                createdBy = "tester"
            )
        )

        val createResponse = mockMvc.post("/v1/projects") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
            header("X-Actor-Id", UUID.randomUUID().toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenant.id,
                    "name" to "ops-summary-project"
                )
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString

        val projectId = objectMapper.readTree(createResponse).get("id").asText()

        mockMvc.get("/v1/projects/$projectId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
        }.andExpect {
            status { isOk() }
        }

        mockMvc.get("/v1/system/operations/summary") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.serviceStatus") { value("UP") }
            jsonPath("$.coreReadP95Ms") { exists() }
            jsonPath("$.coreWriteP95Ms") { exists() }
            jsonPath("$.requestCount") { exists() }
            jsonPath("$.serverErrorRate") { exists() }
            jsonPath("$.deniedCountLast15m") { exists() }
        }

        mockMvc.get("/v1/system/operations/http-metrics") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenant.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)) }
            jsonPath("$.items[0].uri") { exists() }
            jsonPath("$.items[0].method") { exists() }
            jsonPath("$.items[0].count") { exists() }
            jsonPath("$.items[0].p95Ms") { exists() }
        }
    }
}
