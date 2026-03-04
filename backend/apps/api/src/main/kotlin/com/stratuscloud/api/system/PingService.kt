package com.stratuscloud.api.system

import com.stratuscloud.common.trace.TraceIdProvider
import org.springframework.stereotype.Service

// 시스템 상태 확인 응답을 단일 정책으로 생성한다.
@Service
class PingService(
    private val traceIdProvider: TraceIdProvider
) {
    fun ping(): PingResponse {
        return PingResponse(
            message = "pong",
            traceId = traceIdProvider.newTraceId()
        )
    }
}
