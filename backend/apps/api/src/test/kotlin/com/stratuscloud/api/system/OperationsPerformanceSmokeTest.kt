package com.stratuscloud.api.system

import com.fasterxml.jackson.databind.ObjectMapper
import com.stratuscloud.iam.domain.TenantEntity
import com.stratuscloud.iam.repository.TenantRepository
import org.assertj.core.api.Assertions.assertThat
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
import kotlin.math.ceil

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperationsPerformanceSmokeTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Test
    fun `핵심 읽기와 쓰기 API p95가 목표 이하여야 한다`() {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "perf-smoke-tenant",
                createdBy = "tester"
            )
        )

        repeat(WARMUP_COUNT) { index ->
            val projectId = createProject(tenant.id.toString(), "warmup-project-$index")
            getProject(tenant.id.toString(), projectId)
        }

        val writeDurationsMs = mutableListOf<Double>()
        val readDurationsMs = mutableListOf<Double>()

        repeat(SAMPLE_COUNT) { index ->
            val name = "perf-project-$index-${UUID.randomUUID()}"

            val writeStartedAt = System.nanoTime()
            val projectId = createProject(tenant.id.toString(), name)
            writeDurationsMs += elapsedMs(writeStartedAt)

            val readStartedAt = System.nanoTime()
            getProject(tenant.id.toString(), projectId)
            readDurationsMs += elapsedMs(readStartedAt)
        }

        val writeP95 = percentile95(writeDurationsMs)
        val readP95 = percentile95(readDurationsMs)

        assertThat(writeP95).isLessThanOrEqualTo(800.0)
        assertThat(readP95).isLessThanOrEqualTo(300.0)
    }

    private fun createProject(tenantId: String, name: String): String {
        val response = mockMvc.post("/v1/projects") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenantId)
            header("X-Actor-Id", UUID.randomUUID().toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenantId,
                    "name" to name
                )
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString

        return objectMapper.readTree(response).get("id").asText()
    }

    private fun getProject(tenantId: String, projectId: String) {
        mockMvc.get("/v1/projects/$projectId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenantId)
        }.andExpect {
            status { isOk() }
        }
    }

    private fun elapsedMs(startedAt: Long): Double {
        return (System.nanoTime() - startedAt).toDouble() / 1_000_000.0
    }

    private fun percentile95(values: List<Double>): Double {
        val sorted = values.sorted()
        val index = ceil(sorted.size * 0.95).toInt().coerceAtLeast(1) - 1
        return sorted[index]
    }

    companion object {
        private const val WARMUP_COUNT = 5
        private const val SAMPLE_COUNT = 25
    }
}
