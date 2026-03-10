package com.stratuscloud.api.common.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.stratuscloud.api.common.error.ErrorResponse
import com.stratuscloud.common.trace.TraceIdProvider
import com.stratuscloud.iam.auth.AuthMethod
import com.stratuscloud.iam.auth.AuthPrincipal
import com.stratuscloud.iam.domain.RoleType
import com.stratuscloud.iam.exception.UnauthorizedException
import com.stratuscloud.iam.service.ApiKeyService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class AuthContextFilter(
    private val objectMapper: ObjectMapper,
    private val traceIdProvider: TraceIdProvider,
    private val authProperties: AuthProperties,
    private val apiKeyService: ApiKeyService,
    private val jwtDecoder: JwtDecoder
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI.startsWith("/v1/system") ||
            request.requestURI.startsWith("/v1/storage/presigned/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val principal = resolvePrincipal(request)
            AuthContextHolder.set(principal)
            filterChain.doFilter(request, response)
        } catch (ex: UnauthorizedException) {
            unauthorized(response, ex.message ?: "authentication failed")
        } finally {
            AuthContextHolder.clear()
        }
    }

    private fun resolvePrincipal(request: HttpServletRequest): AuthPrincipal {
        if (authProperties.legacyRbacHeaderEnabled) {
            val legacyRole = request.getHeader(HEADER_PROJECT_ROLE)
            if (!legacyRole.isNullOrBlank()) {
                return fromLegacyHeaders(request, legacyRole)
            }
        }

        val apiKey = request.getHeader(authProperties.apiKey.headerName)
        if (!apiKey.isNullOrBlank()) {
            return fromApiKey(apiKey.trim())
        }

        val authorization = request.getHeader(HEADER_AUTHORIZATION)
        if (!authorization.isNullOrBlank() && authorization.startsWith(BEARER_PREFIX)) {
            return fromJwt(authorization.removePrefix(BEARER_PREFIX).trim())
        }

        throw UnauthorizedException("missing credentials")
    }

    private fun fromLegacyHeaders(request: HttpServletRequest, roleHeader: String): AuthPrincipal {
        val role = parseRole(roleHeader)
            ?: throw UnauthorizedException("invalid legacy role header")
        val actorId = parseUuidOrNull(request.getHeader(HEADER_ACTOR_ID)) ?: UUID.randomUUID()
        val tenantId = parseUuidOrNull(request.getHeader(HEADER_TENANT_ID))
        val projectId = parseUuidOrNull(request.getHeader(HEADER_PROJECT_ID))

        val projectRoles = if (projectId != null) mapOf(projectId to setOf(role)) else emptyMap()
        val globalRoles = if (projectId == null) setOf(role) else emptySet()
        return AuthPrincipal(
            actorId = actorId,
            tenantId = tenantId,
            globalRoles = globalRoles,
            projectRoles = projectRoles,
            authMethod = AuthMethod.LEGACY_HEADER
        )
    }

    private fun fromApiKey(rawKey: String): AuthPrincipal {
        val apiKey = apiKeyService.authenticate(rawKey)
        val apiKeyId = requireNotNull(apiKey.id) { "api key id is null" }
        val projectRoles = apiKey.projectId?.let { mapOf(it to setOf(apiKey.role)) } ?: emptyMap()
        val globalRoles = if (apiKey.projectId == null) setOf(apiKey.role) else emptySet()
        return AuthPrincipal(
            actorId = apiKeyId,
            tenantId = apiKey.tenantId,
            globalRoles = globalRoles,
            projectRoles = projectRoles,
            authMethod = AuthMethod.API_KEY,
            apiKeyId = apiKeyId
        )
    }

    private fun fromJwt(token: String): AuthPrincipal {
        val jwt = try {
            jwtDecoder.decode(token)
        } catch (ex: JwtException) {
            throw UnauthorizedException("invalid jwt token")
        }

        val tenantId = parseUuidOrNull(jwt.getClaimAsString(CLAIM_TENANT_ID))
            ?: throw UnauthorizedException("tenant_id claim is required")
        val actorId = parseActorId(jwt)
        val globalRoles = (
            parseRoleSet(jwt.getClaimAsStringList(CLAIM_GLOBAL_ROLES)) +
                parseRoleSet(jwt.getClaimAsStringList(CLAIM_ROLES))
            ).toSet()
        val projectRoles = parseProjectRoles(jwt.claims[CLAIM_PROJECT_ROLES])

        if (globalRoles.isEmpty() && projectRoles.isEmpty()) {
            throw UnauthorizedException("role claims are missing")
        }

        return AuthPrincipal(
            actorId = actorId,
            tenantId = tenantId,
            globalRoles = globalRoles,
            projectRoles = projectRoles,
            authMethod = AuthMethod.JWT
        )
    }

    private fun parseActorId(jwt: Jwt): UUID {
        val subject = jwt.subject ?: jwt.getClaimAsString("sub")
        if (subject.isNullOrBlank()) {
            throw UnauthorizedException("sub claim is required")
        }
        return parseUuidOrNull(subject) ?: UUID.nameUUIDFromBytes(subject.toByteArray(Charsets.UTF_8))
    }

    private fun parseProjectRoles(rawClaim: Any?): Map<UUID, Set<RoleType>> {
        val claim = rawClaim as? Map<*, *> ?: return emptyMap()
        return claim.entries
            .mapNotNull { (projectIdRaw, rolesRaw) ->
                val projectId = parseUuidOrNull(projectIdRaw?.toString()) ?: return@mapNotNull null
                val roles = when (rolesRaw) {
                    is Collection<*> -> parseRoleSet(rolesRaw.mapNotNull { it?.toString() })
                    is String -> parseRoleSet(listOf(rolesRaw))
                    else -> emptySet()
                }
                if (roles.isEmpty()) {
                    null
                } else {
                    projectId to roles
                }
            }
            .toMap()
    }

    private fun parseRoleSet(rawRoles: Collection<String>?): Set<RoleType> {
        return rawRoles.orEmpty()
            .mapNotNull { parseRole(it) }
            .toSet()
    }

    private fun parseRole(raw: String): RoleType? {
        return RoleType.entries.firstOrNull { it.name == raw.trim().uppercase() }
    }

    private fun parseUuidOrNull(raw: String?): UUID? {
        if (raw.isNullOrBlank()) {
            return null
        }
        return runCatching { UUID.fromString(raw.trim()) }.getOrNull()
    }

    private fun unauthorized(response: HttpServletResponse, message: String) {
        // 인증 실패 응답도 공통 에러 포맷으로 내려 프론트/운영 파이프라인을 단순화한다.
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            objectMapper.writeValueAsString(
                ErrorResponse(
                    code = "UNAUTHORIZED",
                    message = message,
                    traceId = traceIdProvider.newTraceId()
                )
            )
        )
    }

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_PROJECT_ROLE = "X-Project-Role"
        private const val HEADER_ACTOR_ID = "X-Actor-Id"
        private const val HEADER_TENANT_ID = "X-Tenant-Id"
        private const val HEADER_PROJECT_ID = "X-Project-Id"
        private const val CLAIM_TENANT_ID = "tenant_id"
        private const val CLAIM_GLOBAL_ROLES = "global_roles"
        private const val CLAIM_ROLES = "roles"
        private const val CLAIM_PROJECT_ROLES = "project_roles"
        private const val BEARER_PREFIX = "Bearer "
    }
}
