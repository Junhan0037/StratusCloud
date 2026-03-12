package com.stratuscloud.api.system

data class OperationsSummaryResponse(
    val serviceStatus: String,
    val coreReadP95Ms: Double?,
    val coreWriteP95Ms: Double?,
    val requestCount: Int,
    val serverErrorRate: Double,
    val deniedCountLast15m: Int
)

data class HttpMetricsResponse(
    val items: List<HttpMetricItemResponse>
)

data class HttpMetricItemResponse(
    val uri: String,
    val method: String,
    val count: Int,
    val maxMs: Double,
    val p95Ms: Double?,
    val errorCount: Int
)
