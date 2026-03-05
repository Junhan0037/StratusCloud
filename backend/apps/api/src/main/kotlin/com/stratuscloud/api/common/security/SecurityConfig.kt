package com.stratuscloud.api.common.security

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableConfigurationProperties(AuthProperties::class)
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .build()
    }

    @Bean
    fun jwtDecoder(authProperties: AuthProperties): JwtDecoder {
        val jwkSetUri = authProperties.oidc.jwkSetUri.trim()
        if (jwkSetUri.isNotBlank()) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
        }
        val issuerUri = authProperties.oidc.issuerUri.trim()
        if (issuerUri.isNotBlank()) {
            return JwtDecoders.fromIssuerLocation(issuerUri)
        }

        // OIDC 설정이 비어있을 때는 명시적으로 인증 실패를 반환한다.
        return JwtDecoder {
            throw BadJwtException("OIDC configuration is missing for JWT validation")
        }
    }
}
