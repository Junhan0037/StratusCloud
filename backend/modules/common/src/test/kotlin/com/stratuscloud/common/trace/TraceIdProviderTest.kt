package com.stratuscloud.common.trace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TraceIdProviderTest {

    @Test
    fun `새로운 trace id는 32자리 소문자 16진수 형식이어야 한다`() {
        val provider = DefaultTraceIdProvider()

        val traceId = provider.newTraceId()

        assertEquals(32, traceId.length)
        assertTrue(traceId.matches(Regex("^[a-f0-9]{32}$")))
    }
}
