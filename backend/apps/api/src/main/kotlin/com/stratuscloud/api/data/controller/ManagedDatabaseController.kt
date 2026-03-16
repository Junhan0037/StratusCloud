package com.stratuscloud.api.data.controller

import com.stratuscloud.api.common.security.ApiAuditRecorder
import com.stratuscloud.api.common.security.AuthContextHolder
import com.stratuscloud.api.common.security.AuthorizationFacade
import com.stratuscloud.api.data.dto.CreateManagedDatabaseRequest
import com.stratuscloud.api.data.dto.ManagedDatabaseResponse
import com.stratuscloud.data.service.ManagedDatabaseService
import com.stratuscloud.iam.service.IamAction
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/data/databases")
class ManagedDatabaseController(
    private val managedDatabaseService: ManagedDatabaseService,
    private val authorizationFacade: AuthorizationFacade,
    private val apiAuditRecorder: ApiAuditRecorder
) {

    @PostMapping
    fun createDatabase(
        @Valid @RequestBody request: CreateManagedDatabaseRequest
    ): ResponseEntity<ManagedDatabaseResponse> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = request.tenantId,
            projectId = request.projectId,
            action = IamAction.DATA_DATABASE_CREATE,
            resource = "project:${request.projectId}",
            resourceType = "MANAGED_DATABASE",
            resourceId = null,
            metadata = mapOf("name" to request.name, "engine" to "POSTGRESQL")
        )
        val created = managedDatabaseService.createDatabase(
            tenantId = request.tenantId,
            projectId = request.projectId,
            name = request.name,
            engineVersion = request.engineVersion,
            instanceClass = request.instanceClass,
            storageGb = request.storageGb,
            actorId = principal.actorId
        )
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = created.tenantId,
            projectId = created.projectId,
            action = IamAction.DATA_DATABASE_CREATE,
            resourceType = "MANAGED_DATABASE",
            resourceId = created.id.toString(),
            metadata = mapOf("name" to created.name, "engine" to created.engine.name)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ManagedDatabaseResponse.from(created))
    }

    @GetMapping
    fun listDatabases(
        @RequestParam tenantId: UUID,
        @RequestParam projectId: UUID
    ): ResponseEntity<List<ManagedDatabaseResponse>> {
        val principal = AuthContextHolder.getRequired()
        authorizationFacade.authorize(
            principal = principal,
            tenantId = tenantId,
            projectId = projectId,
            action = IamAction.DATA_DATABASE_LIST,
            resource = "project:$projectId",
            resourceType = "MANAGED_DATABASE",
            resourceId = null
        )
        return ResponseEntity.ok(
            managedDatabaseService.listDatabases(tenantId, projectId).map(ManagedDatabaseResponse::from)
        )
    }

    @GetMapping("/{databaseId}")
    fun getDatabase(@PathVariable databaseId: UUID): ResponseEntity<ManagedDatabaseResponse> {
        val principal = AuthContextHolder.getRequired()
        val database = managedDatabaseService.getDatabase(databaseId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = database.tenantId,
            projectId = database.projectId,
            action = IamAction.DATA_DATABASE_READ,
            resource = "managed-database:$databaseId",
            resourceType = "MANAGED_DATABASE",
            resourceId = databaseId.toString()
        )
        return ResponseEntity.ok(ManagedDatabaseResponse.from(database))
    }

    @DeleteMapping("/{databaseId}")
    fun deleteDatabase(@PathVariable databaseId: UUID): ResponseEntity<ManagedDatabaseResponse> {
        val principal = AuthContextHolder.getRequired()
        val database = managedDatabaseService.getDatabase(databaseId)
        authorizationFacade.authorize(
            principal = principal,
            tenantId = database.tenantId,
            projectId = database.projectId,
            action = IamAction.DATA_DATABASE_DELETE,
            resource = "managed-database:$databaseId",
            resourceType = "MANAGED_DATABASE",
            resourceId = databaseId.toString()
        )
        val deleted = managedDatabaseService.deleteDatabase(databaseId)
        apiAuditRecorder.recordSuccess(
            principal = principal,
            tenantId = deleted.tenantId,
            projectId = deleted.projectId,
            action = IamAction.DATA_DATABASE_DELETE,
            resourceType = "MANAGED_DATABASE",
            resourceId = databaseId.toString(),
            metadata = mapOf("name" to deleted.name)
        )
        return ResponseEntity.ok(ManagedDatabaseResponse.from(deleted))
    }
}
