package com.stratuscloud.api.common.security

import com.stratuscloud.iam.auth.AuthPrincipal
import com.stratuscloud.iam.exception.UnauthorizedException

object AuthContextHolder {
    private val context: ThreadLocal<AuthPrincipal> = ThreadLocal()

    fun set(principal: AuthPrincipal) {
        context.set(principal)
    }

    fun getOrNull(): AuthPrincipal? = context.get()

    fun getRequired(): AuthPrincipal {
        return getOrNull() ?: throw UnauthorizedException("authentication context is missing")
    }

    fun clear() {
        context.remove()
    }
}
