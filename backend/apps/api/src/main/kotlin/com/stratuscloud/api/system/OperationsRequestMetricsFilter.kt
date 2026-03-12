package com.stratuscloud.api.system

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerMapping

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class OperationsRequestMetricsFilter(
    private val operationsMetricsService: OperationsMetricsService
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !request.requestURI.startsWith("/v1/") ||
            request.requestURI.startsWith("/v1/system/operations") ||
            request.requestURI.startsWith("/v1/system/ping") ||
            request.requestURI.startsWith("/v1/storage/presigned/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startedAt = System.nanoTime()
        try {
            filterChain.doFilter(request, response)
        } finally {
            val matchedPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)?.toString()
                ?: request.requestURI
            val durationMs = (System.nanoTime() - startedAt).toDouble() / 1_000_000.0
            operationsMetricsService.record(
                method = request.method,
                uri = matchedPattern,
                status = response.status,
                durationMs = durationMs
            )
        }
    }
}
