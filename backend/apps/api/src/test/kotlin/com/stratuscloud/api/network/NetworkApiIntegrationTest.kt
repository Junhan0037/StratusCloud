package com.stratuscloud.api.network

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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NetworkApiIntegrationTest {

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
    fun `VPC를 생성하면 기본 route table과 local route가 함께 제공되어야 한다`() {
        val fixture = createFixture("network-core")

        val responseBody = mockMvc.post("/v1/network/vpcs") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "name" to "core-vpc",
                    "cidrBlock" to "10.0.0.0/16"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("core-vpc") }
            jsonPath("$.cidrBlock") { value("10.0.0.0/16") }
            jsonPath("$.defaultRouteTableId") { exists() }
        }.andReturn().response.contentAsString

        val vpcId = objectMapper.readTree(responseBody).get("id").asText()
        val defaultRouteTableId = objectMapper.readTree(responseBody).get("defaultRouteTableId").asText()

        mockMvc.get("/v1/network/vpcs/$vpcId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.defaultRouteTableId") { value(defaultRouteTableId) }
        }

        mockMvc.get("/v1/network/route-tables/$defaultRouteTableId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.vpcId") { value(vpcId) }
            jsonPath("$.isDefault") { value(true) }
            jsonPath("$.routes.length()") { value(1) }
            jsonPath("$.routes[0].destinationCidr") { value("10.0.0.0/16") }
            jsonPath("$.routes[0].targetType") { value("LOCAL") }
        }
    }

    @Test
    fun `subnet과 route table association을 생성하고 조회할 수 있어야 한다`() {
        val fixture = createFixture("network-association")
        val vpcId = createVpc(fixture, "app-vpc", "10.10.0.0/16").get("id").asText()

        val subnetBody = mockMvc.post("/v1/network/subnets") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "vpcId" to vpcId,
                    "name" to "private-a",
                    "cidrBlock" to "10.10.1.0/24",
                    "availabilityZone" to "ap-northeast-2a"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.cidrBlock") { value("10.10.1.0/24") }
            jsonPath("$.routeTableAssociationId") { exists() }
        }.andReturn().response.contentAsString

        val subnet = objectMapper.readTree(subnetBody)
        val routeTableBody = mockMvc.post("/v1/network/route-tables") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "vpcId" to vpcId,
                    "name" to "custom-rt"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("custom-rt") }
            jsonPath("$.isDefault") { value(false) }
        }.andReturn().response.contentAsString

        val routeTableId = objectMapper.readTree(routeTableBody).get("id").asText()

        mockMvc.post("/v1/network/route-tables/$routeTableId/routes") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "destinationCidr" to "0.0.0.0/0",
                    "targetType" to "INTERNET_GATEWAY"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.destinationCidr") { value("0.0.0.0/0") }
            jsonPath("$.targetType") { value("INTERNET_GATEWAY") }
        }

        mockMvc.post("/v1/network/route-tables/$routeTableId/associations") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "subnetId" to subnet.get("id").asText()
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.subnetId") { value(subnet.get("id").asText()) }
            jsonPath("$.routeTableId") { value(routeTableId) }
        }

        mockMvc.get("/v1/network/subnets/${subnet.get("id").asText()}") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.routeTableId") { value(routeTableId) }
        }

        mockMvc.get("/v1/network/route-tables/$routeTableId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.routes.length()") { value(2) }
            jsonPath("$.associations.length()") { value(1) }
            jsonPath("$.associations[0].subnetId") { value(subnet.get("id").asText()) }
        }
    }

    @Test
    fun `VPC 바깥 CIDR subnet 생성과 하위 리소스가 남은 VPC 삭제는 거부되어야 한다`() {
        val fixture = createFixture("network-validation")
        val vpcId = createVpc(fixture, "data-vpc", "10.20.0.0/16").get("id").asText()

        mockMvc.post("/v1/network/subnets") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "vpcId" to vpcId,
                    "name" to "invalid-subnet",
                    "cidrBlock" to "10.99.1.0/24",
                    "availabilityZone" to "ap-northeast-2a"
                )
            )
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("BAD_REQUEST") }
        }

        mockMvc.post("/v1/network/subnets") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "vpcId" to vpcId,
                    "name" to "valid-subnet",
                    "cidrBlock" to "10.20.1.0/24",
                    "availabilityZone" to "ap-northeast-2a"
                )
            )
        }.andExpect {
            status { isCreated() }
        }

        mockMvc.delete("/v1/network/vpcs/$vpcId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("BAD_REQUEST") }
        }
    }

    @Test
    fun `security group rule 교체와 viewer 거부 감사로그가 동작해야 한다`() {
        val fixture = createFixture("network-security")
        val vpcId = createVpc(fixture, "edge-vpc", "10.30.0.0/16").get("id").asText()

        val securityGroupBody = mockMvc.post("/v1/network/security-groups") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "vpcId" to vpcId,
                    "name" to "web-sg",
                    "description" to "web access"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.rules.length()") { value(0) }
        }.andReturn().response.contentAsString

        val securityGroupId = objectMapper.readTree(securityGroupBody).get("id").asText()

        mockMvc.put("/v1/network/security-groups/$securityGroupId/rules") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "rules" to listOf(
                        mapOf(
                            "direction" to "INGRESS",
                            "protocol" to "TCP",
                            "portRangeStart" to 80,
                            "portRangeEnd" to 80,
                            "cidrBlock" to "0.0.0.0/0",
                            "description" to "http"
                        ),
                        mapOf(
                            "direction" to "EGRESS",
                            "protocol" to "ALL",
                            "portRangeStart" to null,
                            "portRangeEnd" to null,
                            "cidrBlock" to "0.0.0.0/0",
                            "description" to "outbound"
                        )
                    )
                )
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.rules.length()") { value(2) }
            jsonPath("$.rules[0].direction") { value("INGRESS") }
        }

        mockMvc.get("/v1/network/security-groups/$securityGroupId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.rules.length()") { value(2) }
        }

        mockMvc.post("/v1/network/security-groups") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "VIEWER")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "vpcId" to vpcId,
                    "name" to "viewer-blocked",
                    "description" to "should fail"
                )
            )
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }

        val deniedLogs = auditEventRepository.search(
            tenantId = fixture.tenant.id,
            projectId = fixture.project.id,
            actorId = null,
            resourceType = "SECURITY_GROUP",
            action = "network:security-group:create",
            result = AuditResult.DENIED,
            occurredFrom = null,
            occurredTo = null
        )
        assertThat(deniedLogs).isNotEmpty
    }

    private fun createVpc(fixture: Fixture, name: String, cidrBlock: String) =
        objectMapper.readTree(
            mockMvc.post("/v1/network/vpcs") {
                contentType = MediaType.APPLICATION_JSON
                header("X-Project-Role", "ADMIN")
                header("X-Tenant-Id", fixture.tenant.id.toString())
                header("X-Project-Id", fixture.project.id.toString())
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "tenantId" to fixture.tenant.id,
                        "projectId" to fixture.project.id,
                        "name" to name,
                        "cidrBlock" to cidrBlock
                    )
                )
            }.andExpect {
                status { isCreated() }
            }.andReturn().response.contentAsString
        )

    private fun createFixture(prefix: String): Fixture {
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
        return Fixture(tenant, project)
    }

    private data class Fixture(
        val tenant: TenantEntity,
        val project: ProjectEntity
    )
}
