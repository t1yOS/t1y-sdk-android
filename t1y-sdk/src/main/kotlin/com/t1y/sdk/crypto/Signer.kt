package com.t1y.sdk.crypto

/**
 * Request signature creation for t1yOS API authentication.
 *
 * ## Signature Algorithm
 *
 * The message to be signed is constructed as:
 * ```
 * METHOD
 * PATH_AND_QUERY
 * SHA256(body)
 * appId
 * timestamp
 * ```
 *
 * The signature is: `HMAC-SHA256(secretKey, message)`
 *
 * This matches the Go server's signature verification exactly.
 */
object Signer {

    /**
     * Computes the HMAC-SHA256 request signature.
     *
     * @param method HTTP method (GET, POST, PUT, DELETE).
     * @param pathAndQuery URL path including query string (e.g., "/v5/meta?field=version").
     * @param body Raw request body string (encrypted if safeMode, or plain JSON; empty string for GET).
     * @param appId Application ID.
     * @param timestamp Current Unix timestamp + offset.
     * @param secretKeyBytes 32-byte secret key.
     * @return 64-character lowercase hex HMAC-SHA256 signature.
     */
    fun createSignature(
        method: String,
        pathAndQuery: String,
        body: String,
        appId: Int,
        timestamp: Long,
        secretKeyBytes: ByteArray
    ): String {
        // Compute SHA-256 of the body
        val bodyHash = Hash.sha256Hex(body)

        // Construct the message to sign: METHOD\n/path?query\nSHA256(body)\nappId\ntimestamp
        val message = listOf(
            method.uppercase(),
            pathAndQuery,
            bodyHash,
            appId.toString(),
            timestamp.toString()
        ).joinToString("\n")

        // Sign with HMAC-SHA256
        return Hmac.hmacSHA256Hex(secretKeyBytes, message)
    }

    /**
     * Returns the current Unix timestamp in seconds, including the client's
     * synchronized time offset.
     *
     * @param offsetSeconds Time offset in seconds (synced from server init).
     * @return Unix timestamp string.
     */
    fun getSafeTimestamp(offsetSeconds: Int): String =
        (System.currentTimeMillis() / 1000 + offsetSeconds).toString()

    /**
     * Returns the current Unix timestamp in seconds as a Long, including offset.
     *
     * @param offsetSeconds Time offset in seconds (synced from server init).
     * @return Unix timestamp as Long.
     */
    fun getSafeTimestampLong(offsetSeconds: Int): Long =
        System.currentTimeMillis() / 1000 + offsetSeconds
}
