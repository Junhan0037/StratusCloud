package com.stratuscloud.api.system

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 운영 점검과 헬스체크 자동화를 위한 기본 엔드포인트다.
@RestController
@RequestMapping("/v1/system")
class SystemPingController(
    private val pingService: PingService
) {

    @GetMapping("/ping")
    fun ping(): ResponseEntity<PingResponse> {
        return ResponseEntity.ok(pingService.ping())
    }
}
