package com.stratuscloud.iam.service

import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class ApiKeySecretHasher {

    fun hash(secret: String): String {
        // API Key 원문은 저장하지 않고 SHA-256 해시만 저장해 유출 위험을 줄인다.
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(secret.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
