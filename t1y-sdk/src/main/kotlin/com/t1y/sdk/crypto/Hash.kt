package com.t1y.sdk.crypto

import java.security.MessageDigest

/**
 * SHA-256 hashing utilities.
 * Uses java.security.MessageDigest for the standard implementation.
 */
object Hash {

    /**
     * Computes the SHA-256 hash of the given string and returns it as a
     * lowercase hexadecimal string.
     *
     * @param data The input string to hash.
     * @return 64-character lowercase hex SHA-256 digest.
     */
    fun sha256Hex(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes the SHA-256 hash of raw bytes and returns a hex string.
     * Used internally by HMAC implementation.
     *
     * @param data The raw bytes to hash.
     * @return 64-character lowercase hex SHA-256 digest.
     */
    fun sha256RawBytes(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
