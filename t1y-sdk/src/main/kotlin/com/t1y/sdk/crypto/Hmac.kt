package com.t1y.sdk.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-SHA256 signing and verification utilities.
 * Uses javax.crypto.Mac for the standard implementation.
 */
object Hmac {

    /**
     * Computes the HMAC-SHA256 of the given [message] using the given [secretKeyBytes]
     * and returns it as a lowercase hexadecimal string.
     *
     * This overload accepts a [ByteArray] to avoid creating a String copy of the
     * secret key. Prefer this for internal use when the key is already in bytes.
     *
     * @param secretKeyBytes The secret key as raw bytes.
     * @param message The message to sign.
     * @return 64-character lowercase hex HMAC-SHA256 digest.
     */
    fun hmacSHA256Hex(secretKeyBytes: ByteArray, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secretKeyBytes, "HmacSHA256")
        mac.init(keySpec)
        val result = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return result.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes the HMAC-SHA256 of the given [message] using the given [secret]
     * and returns it as a lowercase hexadecimal string.
     *
     * @param secret The secret key as a String.
     * @param message The message to sign.
     * @return 64-character lowercase hex HMAC-SHA256 digest.
     */
    fun hmacSHA256Hex(secret: String, message: String): String =
        hmacSHA256Hex(secret.toByteArray(Charsets.UTF_8), message)

    /**
     * Verifies that the given [signature] matches the HMAC-SHA256 of
     * [message] using [secret]. Uses a timing-safe comparison to prevent
     * timing side-channel attacks.
     *
     * @param secret The secret key.
     * @param message The original message.
     * @param signature The expected HMAC-SHA256 hex string.
     * @return true if the signature is valid.
     */
    fun verifyHmacSHA256(secret: String, message: String, signature: String): Boolean {
        if (secret.isEmpty() || message.isEmpty() || signature.isEmpty()) return false
        val expected = hmacSHA256Hex(secret, message)
        return timingSafeEquals(expected.lowercase(), signature.lowercase())
    }

    /**
     * Timing-safe string comparison to prevent timing side-channel attacks.
     * Runs in constant time regardless of where the first mismatch occurs.
     */
    private fun timingSafeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
