package com.stratuscloud.iam.service

import com.stratuscloud.iam.exception.BadRequestException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class SecretCryptoService(
    @Value("\${security.secrets.encryption-key-base64:MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}")
    keyBase64: String
) {

    private val secretKey = SecretKeySpec(decodeKey(keyBase64), ALGORITHM)
    private val secureRandom = SecureRandom()

    fun encrypt(rawValue: String): String {
        if (rawValue.isBlank()) {
            throw BadRequestException("secret value must not be blank")
        }
        val iv = ByteArray(IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val encrypted = cipher.doFinal(rawValue.toByteArray(Charsets.UTF_8))

        val payload = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(encrypted, 0, payload, iv.size, encrypted.size)
        return Base64.getEncoder().encodeToString(payload)
    }

    private fun decodeKey(keyBase64: String): ByteArray {
        val decoded = runCatching { Base64.getDecoder().decode(keyBase64.trim()) }.getOrElse {
            throw IllegalArgumentException("security.secrets.encryption-key-base64 must be valid base64")
        }
        require(decoded.size == 32) { "security.secrets.encryption-key-base64 must decode to 32 bytes" }
        return decoded
    }

    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
    }
}
