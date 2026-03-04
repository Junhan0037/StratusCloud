package com.stratuscloud.api.common.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.stratuscloud.api.common.error.ErrorResponse
import com.stratuscloud.common.trace.TraceIdProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TemporaryRbacFilter(
    private val objectMapper: ObjectMapper,
    private val traceIdProvider: TraceIdProvider
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI.startsWith("/v1/system")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val role = request.getHeader(HEADER_PROJECT_ROLE)

        if (role.isNullOrBlank()) {
            forbidden(response, "project role header is required")
            return
        }

        val normalizedRole = role.uppercase()
        if (normalizedRole !in VALID_ROLES) {
            forbidden(response, "invalid project role")
            return
        }

        val requiresAdmin = request.method != "GET"
        if (requiresAdmin && normalizedRole !in ADMIN_ROLES) {
            forbidden(response, "insufficient role")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun forbidden(response: HttpServletResponse, message: String) {
        // 한국어 설명: 임시 RBAC 실패 시 표준 에러 구조를 일관되게 반환한다.
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            objectMapper.writeValueAsString(
                ErrorResponse(
                    code = "FORBIDDEN",
                    message = message,
                    traceId = traceIdProvider.newTraceId()
                )
            )
        )
    }

    companion object {
        const val HEADER_PROJECT_ROLE = "X-Project-Role"
        private val VALID_ROLES = setOf("OWNER", "ADMIN", "DEVELOPER", "VIEWER")
        private val ADMIN_ROLES = setOf("OWNER", "ADMIN")
    }
}
