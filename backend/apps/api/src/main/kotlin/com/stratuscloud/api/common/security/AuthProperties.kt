package com.stratuscloud.api.common.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security")
class AuthProperties {
    var oidc: Oidc = Oidc()
    var apiKey: ApiKey = ApiKey()
    var legacyRbacHeaderEnabled: Boolean = false

    class Oidc {
        var issuerUri: String = ""
        var jwkSetUri: String = ""
    }

    class ApiKey {
        var headerName: String = "X-API-Key"
    }
}
