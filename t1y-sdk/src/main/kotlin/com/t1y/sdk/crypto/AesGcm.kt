package com.t1y.sdk.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AES-256-GCM encryption and decryption utilities.
 *
 * Payload format (matching the Go server):
 * ```json
 * { "n": "<nonce base64>", "j": "<ciphertext base64>", "t": "<tag base64>" }
 * ```
 *
 * Uses javax.crypto which is available on both JVM and Android (API 21+).
 * No external crypto dependencies required.
 */
object AesGcm {

    /** Nonce length in bytes for AES-256-GCM. */
    private const val NONCE_LENGTH = 12

    /** Authentication tag length in bytes. */
    private const val TAG_LENGTH = 16

    /** Key length in bytes for AES-256. */
    private const val KEY_LENGTH = 32

    @Serializable
    data class EncryptedPayload(
        val n: String, // base64-encoded nonce
        val j: String, // base64-encoded ciphertext
        val t: String  // base64-encoded authentication tag
    )

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Encrypts the given plaintext [data] using AES-256-GCM with the given [keyBytes].
     *
     * @param data The plaintext JSON string to encrypt.
     * @param keyBytes 32-byte encryption key (the SecretKey as UTF-8 bytes).
     * @return JSON string in `{ n, j, t }` format.
     */
    fun encrypt(data: String, keyBytes: ByteArray): String {
        require(keyBytes.size == KEY_LENGTH) {
            "Key length must be $KEY_LENGTH bytes for AES-256-GCM, got: ${keyBytes.size}"
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")

        // Generate random nonce
        val nonce = ByteArray(NONCE_LENGTH)
        SecureRandom().nextBytes(nonce)

        val gcmSpec = GCMParameterSpec(TAG_LENGTH * 8, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        // cipher.doFinal returns ciphertext + tag appended
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        // Split: ciphertext is everything except the last TAG_LENGTH bytes
        val ciphertext = encrypted.copyOf(encrypted.size - TAG_LENGTH)
        val tag = encrypted.copyOfRange(encrypted.size - TAG_LENGTH, encrypted.size)

        val payload = EncryptedPayload(
            n = Base64.getEncoder().encodeToString(nonce),
            j = Base64.getEncoder().encodeToString(ciphertext),
            t = Base64.getEncoder().encodeToString(tag)
        )

        return json.encodeToString(EncryptedPayload.serializer(), payload)
    }

    /**
     * Decrypts a JSON payload in `{ n, j, t }` format using AES-256-GCM.
     *
     * @param jsonPayload The encrypted JSON string with `n`, `j`, `t` fields.
     * @param keyBytes 32-byte encryption key (the SecretKey as UTF-8 bytes).
     * @return The decrypted plaintext JSON string.
     */
    fun decrypt(jsonPayload: String, keyBytes: ByteArray): String {
        require(keyBytes.size == KEY_LENGTH) {
            "Key length must be $KEY_LENGTH bytes for AES-256-GCM, got: ${keyBytes.size}"
        }

        val payload = json.decodeFromString(EncryptedPayload.serializer(), jsonPayload)
        val nonce = Base64.getDecoder().decode(payload.n)
        val ciphertext = Base64.getDecoder().decode(payload.j)
        val tag = Base64.getDecoder().decode(payload.t)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH * 8, nonce)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        // Reconstruct: ciphertext + tag
        val sealed = ciphertext + tag
        val decrypted = cipher.doFinal(sealed)
        return String(decrypted, Charsets.UTF_8)
    }
}
