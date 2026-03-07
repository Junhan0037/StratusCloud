package com.stratuscloud.audit.repository

import com.stratuscloud.audit.domain.AuditEventEntity
import com.stratuscloud.audit.domain.AuditResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface AuditEventRepository : JpaRepository<AuditEventEntity, UUID> {

    @Query(
        """
        select event
        from AuditEventEntity event
        where (:tenantId is null or event.tenantId = :tenantId)
          and (:projectId is null or event.projectId = :projectId)
          and (:actorId is null or event.actorId = :actorId)
          and (:resourceType is null or event.resourceType = :resourceType)
          and (:action is null or event.action = :action)
          and (:result is null or event.result = :result)
          and (:occurredFrom is null or event.occurredAt >= :occurredFrom)
          and (:occurredTo is null or event.occurredAt <= :occurredTo)
        order by event.occurredAt desc
        """
    )
    fun search(
        @Param("tenantId") tenantId: UUID?,
        @Param("projectId") projectId: UUID?,
        @Param("actorId") actorId: UUID?,
        @Param("resourceType") resourceType: String?,
        @Param("action") action: String?,
        @Param("result") result: AuditResult?,
        @Param("occurredFrom") occurredFrom: LocalDateTime?,
        @Param("occurredTo") occurredTo: LocalDateTime?
    ): List<AuditEventEntity>
}
