package com.stratuscloud.api.storage

import com.fasterxml.jackson.databind.JsonNode
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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.net.URI

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "storage.root-path=build/test-storage/storage-api"
    ]
)
class StorageApiIntegrationTest {

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
    fun `버킷 생성 후 presigned url로 오브젝트를 업로드하고 다운로드할 수 있어야 한다`() {
        val fixture = createFixture("storage-happy")
        val bucket = createBucket(fixture, "artifact-bucket")

        mockMvc.get("/v1/storage/buckets/${bucket.get("id").asText()}") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("artifact-bucket") }
            jsonPath("$.acl") { value("PRIVATE") }
            jsonPath("$.objectCount") { value(0) }
        }

        val uploadPresign = createPresign(
            fixture = fixture,
            bucketId = bucket.get("id").asText(),
            operation = "UPLOAD",
            key = "release/app.txt",
            objectContentType = "text/plain"
        )

        val uploadUri = URI(uploadPresign.get("url").asText())
        mockMvc.put(uploadUri.rawPath + "?" + uploadUri.rawQuery) {
            contentType = MediaType.TEXT_PLAIN
            content = "hello-object-storage".toByteArray()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.key") { value("release/app.txt") }
            jsonPath("$.contentType") { value("text/plain") }
            jsonPath("$.sizeBytes") { value(20) }
            jsonPath("$.acl") { value("PRIVATE") }
        }

        val objectsResponse = mockMvc.get("/v1/storage/buckets/${bucket.get("id").asText()}/objects") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].key") { value("release/app.txt") }
        }.andReturn().response.contentAsString

        val objectId = objectMapper.readTree(objectsResponse)[0].get("id").asText()

        val downloadPresign = createPresign(
            fixture = fixture,
            bucketId = bucket.get("id").asText(),
            operation = "DOWNLOAD",
            key = "release/app.txt"
        )

        val downloadUri = URI(downloadPresign.get("url").asText())
        mockMvc.get(downloadUri.rawPath + "?" + downloadUri.rawQuery)
            .andExpect {
                status { isOk() }
                content { bytes("hello-object-storage".toByteArray()) }
                header { string("Content-Type", "text/plain") }
            }

        mockMvc.delete("/v1/storage/objects/$objectId") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.delete("/v1/storage/buckets/${bucket.get("id").asText()}") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `오브젝트가 남아 있는 버킷 삭제와 만료된 presigned url 사용은 거부되어야 한다`() {
        val fixture = createFixture("storage-guard")
        val bucket = createBucket(fixture, "guard-bucket")

        val uploadPresign = createPresign(
            fixture = fixture,
            bucketId = bucket.get("id").asText(),
            operation = "UPLOAD",
            key = "reports/daily.json",
            objectContentType = "application/json",
            expiresInSeconds = 1
        )

        val uploadUri = URI(uploadPresign.get("url").asText())
        mockMvc.put(uploadUri.rawPath + "?" + uploadUri.rawQuery) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"ok"}""".toByteArray()
        }.andExpect {
            status { isCreated() }
        }

        mockMvc.delete("/v1/storage/buckets/${bucket.get("id").asText()}") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("BAD_REQUEST") }
        }

        val expiredPresign = createPresign(
            fixture = fixture,
            bucketId = bucket.get("id").asText(),
            operation = "DOWNLOAD",
            key = "reports/daily.json",
            expiresInSeconds = 0
        )

        val expiredUri = URI(expiredPresign.get("url").asText())
        mockMvc.get(expiredUri.rawPath + "?" + expiredUri.rawQuery)
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("UNAUTHORIZED") }
            }
    }

    @Test
    fun `viewer는 버킷 생성이 거부되고 감사 로그에 denied가 남아야 한다`() {
        val fixture = createFixture("storage-denied")

        mockMvc.post("/v1/storage/buckets") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "VIEWER")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "name" to "blocked-bucket",
                    "acl" to "PRIVATE"
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
            resourceType = "BUCKET",
            action = "storage:bucket:create",
            result = AuditResult.DENIED,
            occurredFrom = null,
            occurredTo = null
        )
        assertThat(deniedLogs).isNotEmpty
    }

    @Test
    fun `다른 tenant 사용자는 버킷을 조회할 수 없고 denied audit가 남아야 한다`() {
        val ownerFixture = createFixture("storage-tenant-owner")
        val attackerFixture = createFixture("storage-tenant-attacker")
        val bucket = createBucket(ownerFixture, "isolated-bucket")

        mockMvc.get("/v1/storage/buckets/${bucket.get("id").asText()}") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", attackerFixture.tenant.id.toString())
            header("X-Project-Id", attackerFixture.project.id.toString())
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }

        val deniedLogs = auditEventRepository.search(
            tenantId = ownerFixture.tenant.id,
            projectId = ownerFixture.project.id,
            actorId = null,
            resourceType = "BUCKET",
            action = "storage:bucket:read",
            result = AuditResult.DENIED,
            occurredFrom = null,
            occurredTo = null
        )
        assertThat(deniedLogs).isNotEmpty
    }

    @Test
    fun `스토리지 정책을 설정하고 버킷 quota를 초과하면 생성이 거부되어야 한다`() {
        val fixture = createFixture("storage-governance-quota")

        mockMvc.put("/v1/governance/storage/policies/projects/${fixture.project.id}") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "maxBucketCount" to 1,
                    "maxObjectCount" to 100,
                    "maxTotalBytes" to 1048576,
                    "presignPerMinute" to 10,
                    "uploadPerMinute" to 10,
                    "downloadPerMinute" to 10
                )
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.projectId") { value(fixture.project.id.toString()) }
            jsonPath("$.maxBucketCount") { value(1) }
        }

        createBucket(fixture, "quota-bucket-a")

        mockMvc.post("/v1/storage/buckets") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "name" to "quota-bucket-b",
                    "acl" to "PRIVATE"
                )
            )
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("BAD_REQUEST") }
            jsonPath("$.message") { value("bucket quota exceeded for project: ${fixture.project.id}") }
        }
    }

    @Test
    fun `분당 presign 요청 제한을 넘으면 too many requests가 반환되어야 한다`() {
        val fixture = createFixture("storage-governance-rate")
        val bucket = createBucket(fixture, "rate-bucket")

        mockMvc.put("/v1/governance/storage/policies/projects/${fixture.project.id}") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "maxBucketCount" to 10,
                    "maxObjectCount" to 100,
                    "maxTotalBytes" to 1048576,
                    "presignPerMinute" to 1,
                    "uploadPerMinute" to 10,
                    "downloadPerMinute" to 10
                )
            )
        }.andExpect {
            status { isOk() }
        }

        createPresign(
            fixture = fixture,
            bucketId = bucket.get("id").asText(),
            operation = "UPLOAD",
            key = "rate/object-a.txt",
            objectContentType = "text/plain"
        )

        mockMvc.post("/v1/storage/buckets/${bucket.get("id").asText()}/objects:presign") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id.toString(),
                    "projectId" to fixture.project.id.toString(),
                    "operation" to "UPLOAD",
                    "key" to "rate/object-b.txt",
                    "expiresInSeconds" to 900,
                    "contentType" to "text/plain"
                )
            )
        }.andExpect {
            status { isTooManyRequests() }
            jsonPath("$.code") { value("TOO_MANY_REQUESTS") }
        }
    }

    @Test
    fun `버킷과 오브젝트 태그를 저장하고 다시 조회할 수 있어야 한다`() {
        val fixture = createFixture("storage-governance-tags")
        val bucket = createBucket(fixture, "tag-bucket")
        val uploadPresign = createPresign(
            fixture = fixture,
            bucketId = bucket.get("id").asText(),
            operation = "UPLOAD",
            key = "tagged/app.txt",
            objectContentType = "text/plain"
        )
        val uploadUri = URI(uploadPresign.get("url").asText())
        mockMvc.put(uploadUri.rawPath + "?" + uploadUri.rawQuery) {
            contentType = MediaType.TEXT_PLAIN
            content = "tagged-object".toByteArray()
        }.andExpect {
            status { isCreated() }
        }

        val objectsResponse = mockMvc.get("/v1/storage/buckets/${bucket.get("id").asText()}/objects") {
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andReturn().response.contentAsString
        val objectId = objectMapper.readTree(objectsResponse)[0].get("id").asText()

        mockMvc.put("/v1/storage/buckets/${bucket.get("id").asText()}/tags") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(mapOf("tags" to listOf("backup", "cold")))
        }.andExpect {
            status { isOk() }
            jsonPath("$.tags.length()") { value(2) }
        }

        mockMvc.put("/v1/storage/objects/$objectId/tags") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(mapOf("tags" to listOf("release", "artifact")))
        }.andExpect {
            status { isOk() }
            jsonPath("$.tags.length()") { value(2) }
        }

        mockMvc.get("/v1/storage/buckets/${bucket.get("id").asText()}/tags") {
            header("X-Project-Role", "VIEWER")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.tags[0]") { value("backup") }
            jsonPath("$.tags[1]") { value("cold") }
        }

        mockMvc.get("/v1/storage/objects/$objectId/tags") {
            header("X-Project-Role", "VIEWER")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.tags[0]") { value("release") }
            jsonPath("$.tags[1]") { value("artifact") }
        }
    }

    @Test
    fun `업로드와 다운로드 후 프로젝트와 버킷 미터링을 조회할 수 있어야 한다`() {
        val fixture = createFixture("storage-governance-metering")
        val bucket = createBucket(fixture, "metering-bucket")
        val uploadPresign = createPresign(
            fixture = fixture,
            bucketId = bucket.get("id").asText(),
            operation = "UPLOAD",
            key = "metering/data.txt",
            objectContentType = "text/plain"
        )
        val uploadUri = URI(uploadPresign.get("url").asText())
        mockMvc.put(uploadUri.rawPath + "?" + uploadUri.rawQuery) {
            contentType = MediaType.TEXT_PLAIN
            content = "metering-body".toByteArray()
        }.andExpect {
            status { isCreated() }
        }

        val downloadPresign = createPresign(
            fixture = fixture,
            bucketId = bucket.get("id").asText(),
            operation = "DOWNLOAD",
            key = "metering/data.txt"
        )
        val downloadUri = URI(downloadPresign.get("url").asText())
        mockMvc.get(downloadUri.rawPath + "?" + downloadUri.rawQuery)
            .andExpect {
                status { isOk() }
            }

        mockMvc.get("/v1/governance/storage/metering/projects/${fixture.project.id}") {
            header("X-Project-Role", "VIEWER")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.projectId") { value(fixture.project.id.toString()) }
            jsonPath("$.bucketCount") { value(1) }
            jsonPath("$.objectCount") { value(1) }
            jsonPath("$.storedBytes") { value(13) }
            jsonPath("$.uploadedBytes") { value(13) }
            jsonPath("$.downloadedBytes") { value(13) }
        }

        mockMvc.get("/v1/governance/storage/metering/buckets/${bucket.get("id").asText()}") {
            header("X-Project-Role", "VIEWER")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.bucketId") { value(bucket.get("id").asText()) }
            jsonPath("$.objectCount") { value(1) }
            jsonPath("$.storedBytes") { value(13) }
            jsonPath("$.uploadedBytes") { value(13) }
            jsonPath("$.downloadedBytes") { value(13) }
        }
    }

    private fun createBucket(fixture: Fixture, name: String): JsonNode {
        val response = mockMvc.post("/v1/storage/buckets") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tenantId" to fixture.tenant.id,
                    "projectId" to fixture.project.id,
                    "name" to name,
                    "acl" to "PRIVATE"
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value(name) }
        }.andReturn().response.contentAsString
        return objectMapper.readTree(response)
    }

    private fun createPresign(
        fixture: Fixture,
        bucketId: String,
        operation: String,
        key: String,
        objectContentType: String? = null,
        expiresInSeconds: Int = 900
    ): JsonNode {
        val response = mockMvc.post("/v1/storage/buckets/$bucketId/objects:presign") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Project-Role", "ADMIN")
            header("X-Tenant-Id", fixture.tenant.id.toString())
            header("X-Project-Id", fixture.project.id.toString())
            content = objectMapper.writeValueAsString(
                buildMap<String, Any> {
                    put("tenantId", fixture.tenant.id.toString())
                    put("projectId", fixture.project.id.toString())
                    put("operation", operation)
                    put("key", key)
                    put("expiresInSeconds", expiresInSeconds)
                    objectContentType?.let { put("contentType", it) }
                }
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.operation") { value(operation) }
            jsonPath("$.url") { exists() }
        }.andReturn().response.contentAsString
        return objectMapper.readTree(response)
    }

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
