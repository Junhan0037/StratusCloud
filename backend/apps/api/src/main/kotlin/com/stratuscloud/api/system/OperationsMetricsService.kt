package com.stratuscloud.api.system

import com.stratuscloud.audit.domain.AuditResult
import com.stratuscloud.audit.repository.AuditEventRepository
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.round

@Service
class OperationsMetricsService(
    private val healthEndpoint: HealthEndpoint,
    private val auditEventRepository: AuditEventRepository
) {
    private val lock = Any()
    private val samples = ArrayDeque<RequestSample>()

    fun record(method: String, uri: String, status: Int, durationMs: Double) {
        synchronized(lock) {
            samples.addLast(
                RequestSample(
                    method = method,
                    uri = uri,
                    status = status,
                    durationMs = durationMs,
                    occurredAt = LocalDateTime.now()
                )
            )
            while (samples.size > MAX_SAMPLE_COUNT) {
                samples.removeFirst()
            }
        }
    }

    fun summary(tenantId: UUID?): OperationsSummaryResponse {
        val currentSamples = snapshot()
        val serverErrors = currentSamples.count { it.status >= 500 }

        return OperationsSummaryResponse(
            serviceStatus = healthEndpoint.health().status.code,
            coreReadP95Ms = percentile(currentSamples.filter { it.method == "GET" && it.uri == CORE_READ_URI }.map { it.durationMs }),
            coreWriteP95Ms = percentile(currentSamples.filter { it.method == "POST" && it.uri == CORE_WRITE_URI }.map { it.durationMs }),
            requestCount = currentSamples.size,
            serverErrorRate = percentage(serverErrors, currentSamples.size),
            deniedCountLast15m = auditEventRepository.search(
                tenantId = tenantId,
                projectId = null,
                actorId = null,
                resourceType = null,
                action = null,
                result = AuditResult.DENIED,
                occurredFrom = LocalDateTime.now().minusMinutes(15),
                occurredTo = null
            ).size
        )
    }

    fun httpMetrics(): HttpMetricsResponse {
        val items = snapshot()
            .groupBy { it.method to it.uri }
            .map { (key, groupedSamples) ->
                HttpMetricItemResponse(
                    method = key.first,
                    uri = key.second,
                    count = groupedSamples.size,
                    maxMs = groupedSamples.maxOfOrNull { it.durationMs }?.rounded() ?: 0.0,
                    p95Ms = percentile(groupedSamples.map { it.durationMs }),
                    errorCount = groupedSamples.count { it.status >= 500 }
                )
            }
            .sortedWith(compareByDescending<HttpMetricItemResponse> { it.count }.thenBy { it.method }.thenBy { it.uri })

        return HttpMetricsResponse(items = items)
    }

    private fun snapshot(): List<RequestSample> {
        return synchronized(lock) { samples.toList() }
    }

    private fun percentile(values: List<Double>): Double? {
        if (values.isEmpty()) {
            return null
        }
        val sorted = values.sorted()
        val index = ceil(sorted.size * 0.95).toInt().coerceAtLeast(1) - 1
        return sorted[index].rounded()
    }

    private fun percentage(numerator: Int, denominator: Int): Double {
        if (denominator == 0) {
            return 0.0
        }
        return ((numerator.toDouble() / denominator.toDouble()) * 100.0).rounded()
    }

    private fun Double.rounded(): Double = round(this * 100.0) / 100.0

    private data class RequestSample(
        val method: String,
        val uri: String,
        val status: Int,
        val durationMs: Double,
        val occurredAt: LocalDateTime
    )

    companion object {
        private const val MAX_SAMPLE_COUNT = 2_000
        private const val CORE_READ_URI = "/v1/projects/{projectId}"
        private const val CORE_WRITE_URI = "/v1/projects"
    }
}
