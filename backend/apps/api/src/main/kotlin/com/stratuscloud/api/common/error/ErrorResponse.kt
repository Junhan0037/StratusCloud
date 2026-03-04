package com.stratuscloud.api.common.error

data class ErrorResponse(
    val code: String,
    val message: String,
    val traceId: String,
    val details: Map<String, Any?> = emptyMap()
)
