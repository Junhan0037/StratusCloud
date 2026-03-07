package com.stratuscloud.api.compute

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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComputeAutoscalingApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    fun `오토스케일 그룹을 생성하고 메트릭 평가로 스케일 아웃과 인을 수행할 수 있어야 한다`() {
        val fixture = createFixture("autoscaling")
        val imageId = createImage(fixture.tenant.id.toString(), "ubuntu-autoscale")

        val groupBody = mockMvc.post("/v1/compute/autoscaling-groups") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "name" to "web-asg",
                    "imageId" to imageId,
                    "flavor" to "small",
                    "minInstances" to 1,
                    "maxInstances" to 3,
                    "cpuScaleOutPercent" to 70,
                    "cpuScaleInPercent" to 25,
                    "memoryScaleOutPercent" to 80,
                    "memoryScaleInPercent" to 30
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("web-asg") }
            jsonPath("$.desiredInstances") { value(1) }
            jsonPath("$.currentInstances") { value(1) }
            jsonPath("$.healthPolicy") { value("RESTART") }
        }.andReturn().response.contentAsString

        val groupId = objectMapper.readTree(groupBody).get("id").asText()

        val instancesAfterCreate = mockMvc.get("/v1/compute/instances") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            param("tenantId", fixture.tenant.id.toString())
            param("projectId", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].autoscalingGroupId") { value(groupId) }
            jsonPath("$[0].healthStatus") { value("UNKNOWN") }
            jsonPath("$[0].restartCount") { value(0) }
        }.andReturn().response.contentAsString

        val firstInstanceId = objectMapper.readTree(instancesAfterCreate)[0].get("id").asText()

        mockMvc.post("/v1/compute/instances/$firstInstanceId/metrics") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "cpuPercent" to 91,
                    "memoryPercent" to 82
                )
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.cpuPercent") { value(91) }
            jsonPath("$.memoryPercent") { value(82) }
        }

        mockMvc.post("/v1/compute/autoscaling-groups/$groupId:evaluate") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.desiredInstances") { value(2) }
            jsonPath("$.currentInstances") { value(2) }
        }

        val instancesAfterScaleOut = mockMvc.get("/v1/compute/instances") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            param("tenantId", fixture.tenant.id.toString())
            param("projectId", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
        }.andReturn().response.contentAsString

        objectMapper.readTree(instancesAfterScaleOut).forEach { instanceNode ->
            mockMvc.post("/v1/compute/instances/${instanceNode.get("id").asText()}/metrics") {
                contentType = MediaType.APPLICATION_JSON
                header("X-Project-Role", "ADMIN")
                header("X-Tenant-Id", fixture.tenant.id.toString())
                header("X-Project-Id", fixture.project.id.toString())
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "cpuPercent" to 10,
                        "memoryPercent" to 12
                    )
                )
            }.andExpect {
                status { isOk() }
            }
        }

        mockMvc.post("/v1/compute/autoscaling-groups/$groupId:evaluate") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.desiredInstances") { value(1) }
            jsonPath("$.currentInstances") { value(1) }
        }
    }

    @Test
    fun `헬스체크 실패가 누적되면 재시작 우선 정책으로 복구해야 한다`() {
        val fixture = createFixture("healthcheck")
        val imageId = createImage(fixture.tenant.id.toString(), "ubuntu-health")

        val groupBody = mockMvc.post("/v1/compute/autoscaling-groups") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "name" to "worker-asg",
                    "imageId" to imageId,
                    "flavor" to "nano",
                    "minInstances" to 1,
                    "maxInstances" to 2,
                    "cpuScaleOutPercent" to 80,
                    "cpuScaleInPercent" to 20,
                    "memoryScaleOutPercent" to 80,
                    "memoryScaleInPercent" to 20,
                    "failureThreshold" to 3
                )
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString

        val groupId = objectMapper.readTree(groupBody).get("id").asText()

        val instancesBody = mockMvc.get("/v1/compute/instances") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            param("tenantId", fixture.tenant.id.toString())
            param("projectId", fixture.project.id.toString())
        }.andReturn().response.contentAsString

        val instanceId = objectMapper.readTree(instancesBody)[0].get("id").asText()

        repeat(3) {
            mockMvc.post("/v1/compute/instances/$instanceId/health") {
                contentType = MediaType.APPLICATION_JSON
                header("X-Project-Role", "ADMIN")
                header("X-Tenant-Id", fixture.tenant.id.toString())
                header("X-Project-Id", fixture.project.id.toString())
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "status" to "UNHEALTHY",
                        "detail" to "probe-timeout"
                    )
                )
            }.andExpect {
                status { isOk() }
                jsonPath("$.status") { value("UNHEALTHY") }
            }
        }

        mockMvc.post("/v1/compute/autoscaling-groups/$groupId:reconcile-health") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.restartedInstanceIds[0]") { value(instanceId) }
            jsonPath("$.replacementInstanceIds.length()") { value(0) }
        }

        mockMvc.get("/v1/compute/instances/$instanceId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("RUNNING") }
            jsonPath("$.healthStatus") { value("HEALTHY") }
            jsonPath("$.restartCount") { value(1) }
        }
    }

    @Test
    fun `뷰어는 오토스케일 그룹 변경과 메트릭 입력이 거부되고 감사로그에 남아야 한다`() {
        val fixture = createFixture("autoscaling-denied")
        val imageId = createImage(fixture.tenant.id.toString(), "ubuntu-denied")

        val deniedResponse = mockMvc.post("/v1/compute/autoscaling-groups") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "VIEWER")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "name" to "viewer-blocked-asg",
                    "imageId" to imageId,
                    "flavor" to "small",
                    "minInstances" to 1,
                    "maxInstances" to 2,
                    "cpuScaleOutPercent" to 70,
                    "cpuScaleInPercent" to 30,
                    "memoryScaleOutPercent" to 75,
                    "memoryScaleInPercent" to 30
                )
            )
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }.andReturn().response.contentAsString

        check(deniedResponse.contains("FORBIDDEN"))

        mockMvc.get("/v1/audit/logs") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            param("tenantId", fixture.tenant.id.toString())
            param("projectId", fixture.project.id.toString())
            param("resourceType", "COMPUTE_AUTOSCALING_GROUP")
            param("result", "DENIED")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].action") { value("compute:autoscaling-group:create") }
            jsonPath("$[0].result") { value("DENIED") }
        }
    }

    private fun createFixture(prefix: String): ComputeFixture {
        val tenant = tenantRepository.save(
            TenantEntity(
                name = "$prefix-tenant",
                createdBy = "tester"
            )
        )
        val project = projectRepository.save(
            ProjectEntity(
                tenantId = requireNotNull(tenant.id),
                name = "$prefix-project",
                createdBy = "tester"
            )
        )
        return ComputeFixture(tenant, project)
    }

    private fun createImage(tenantId: String, name: String): String {
        val response = mockMvc.post("/v1/compute/images") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", tenantId)
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to tenantId,
                    "name" to name,
                    "version" to "2026.04",
                    "osType" to "LINUX",
                    "tags" to listOf("stable")
                )
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString

        return objectMapper.readTree(response).get("id").asText()
    }

    private data class ComputeFixture(
        val tenant: TenantEntity,
        val project: ProjectEntity
    )
}
