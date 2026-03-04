package com.stratuscloud.common.trace

import org.springframework.stereotype.Component
import java.security.SecureRandom

// 외부 추적 시스템 연동 전까지 사용할 기본 trace id 생성기다.
@Component
class DefaultTraceIdProvider : TraceIdProvider {

    private val secureRandom = SecureRandom()

    override fun newTraceId(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }
}
