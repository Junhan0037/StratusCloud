package com.stratuscloud.common.trace

interface TraceIdProvider {
    fun newTraceId(): String
}
