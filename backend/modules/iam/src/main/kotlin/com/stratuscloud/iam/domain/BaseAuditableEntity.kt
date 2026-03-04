package com.stratuscloud.iam.domain

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseAuditableEntity(
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "created_by", nullable = false, length = 100)
    var createdBy: String = "system"
) {
    @PrePersist
    fun prePersist() {
        // 한국어 설명: 생성 시점에 감사 필드를 자동으로 채운다.
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        // 한국어 설명: 수정 시점마다 updatedAt을 갱신한다.
        updatedAt = LocalDateTime.now()
    }
}
