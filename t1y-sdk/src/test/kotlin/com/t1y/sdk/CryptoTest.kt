package com.t1y.sdk

import com.t1y.sdk.crypto.AesGcm
import com.t1y.sdk.crypto.Hash
import com.t1y.sdk.crypto.Hmac
import com.t1y.sdk.crypto.Signer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class CryptoTest {

    // ===== SHA-256 =====

    @Test
    fun `sha256Hex returns 64 char hex string`() {
        val result = Hash.sha256Hex("hello")
        assertEquals(64, result.length)
    }

    @Test
    fun `sha256Hex is deterministic`() {
        val a = Hash.sha256Hex("hello")
        val b = Hash.sha256Hex("hello")
        assertEquals(a, b)
    }

    @Test
    fun `sha256Hex different inputs produce different hashes`() {
        val a = Hash.sha256Hex("hello")
        val b = Hash.sha256Hex("world")
        assertNotEquals(a, b)
    }

    @Test
    fun `sha256Hex empty string produces known hash`() {
        // SHA-256 of empty string: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val result = Hash.sha256Hex("")
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            result
        )
    }

    @Test
    fun `sha256Hex handles UTF-8 characters`() {
        val result = Hash.sha256Hex("你好世界")
        assertEquals(64, result.length)
    }

    // ===== HMAC-SHA256 =====

    @Test
    fun `hmacSHA256Hex returns 64 char hex string`() {
        val result = Hmac.hmacSHA256Hex("secret", "message")
        assertEquals(64, result.length)
    }

    @Test
    fun `hmacSHA256Hex is deterministic`() {
        val a = Hmac.hmacSHA256Hex("secret", "message")
        val b = Hmac.hmacSHA256Hex("secret", "message")
        assertEquals(a, b)
    }

    @Test
    fun `hmacSHA256Hex different keys produce different hashes`() {
        val a = Hmac.hmacSHA256Hex("secret1", "message")
        val b = Hmac.hmacSHA256Hex("secret2", "message")
        assertNotEquals(a, b)
    }

    @Test
    fun `hmacSHA256Hex different messages produce different hashes`() {
        val a = Hmac.hmacSHA256Hex("secret", "hello")
        val b = Hmac.hmacSHA256Hex("secret", "world")
        assertNotEquals(a, b)
    }

    // ===== HMAC Verification =====

    @Test
    fun `verifyHmacSHA256 returns true for valid signature`() {
        val signature = Hmac.hmacSHA256Hex("secret", "message")
        assertTrue(Hmac.verifyHmacSHA256("secret", "message", signature))
    }

    @Test
    fun `verifyHmacSHA256 returns false for invalid signature`() {
        assertFalse(Hmac.verifyHmacSHA256("secret", "message", "a".repeat(64)))
    }

    @Test
    fun `verifyHmacSHA256 is case insensitive`() {
        val signature = Hmac.hmacSHA256Hex("secret", "message")
        assertTrue(Hmac.verifyHmacSHA256("secret", "message", signature.uppercase()))
    }

    @Test
    fun `verifyHmacSHA256 returns false for wrong length signature`() {
        assertFalse(Hmac.verifyHmacSHA256("secret", "message", "abc"))
    }

    @Test
    fun `verifyHmacSHA256 returns false for empty inputs`() {
        assertFalse(Hmac.verifyHmacSHA256("", "message", "a".repeat(64)))
        assertFalse(Hmac.verifyHmacSHA256("secret", "", "a".repeat(64)))
        assertFalse(Hmac.verifyHmacSHA256("secret", "message", ""))
    }

    // ===== AES-256-GCM =====

    @Test
    fun `AES encrypt and decrypt round trip`() {
        val key = "a".repeat(32).toByteArray(Charsets.UTF_8)
        val plaintext = """{"name":"Alice","age":25}"""

        val encrypted = AesGcm.encrypt(plaintext, key)
        val decrypted = AesGcm.decrypt(encrypted, key)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `AES encrypted payload has n, j, t fields`() {
        val key = "a".repeat(32).toByteArray(Charsets.UTF_8)
        val encrypted = AesGcm.encrypt("test data", key)

        // Parse and verify structure
        val json = Json.parseToJsonElement(encrypted)
        assertTrue(json is kotlinx.serialization.json.JsonObject)
        val obj = json as kotlinx.serialization.json.JsonObject
        assertTrue(obj.containsKey("n"))
        assertTrue(obj.containsKey("j"))
        assertTrue(obj.containsKey("t"))
    }

    @Test
    fun `AES different nonces produce different ciphertexts`() {
        val key = "a".repeat(32).toByteArray(Charsets.UTF_8)
        val plaintext = "same plaintext"

        val encrypted1 = AesGcm.encrypt(plaintext, key)
        val encrypted2 = AesGcm.encrypt(plaintext, key)

        // Different nonces → different encrypted output
        assertNotEquals(encrypted1, encrypted2)
    }

    @Test
    fun `AES wrong key length throws`() {
        assertThrows<IllegalArgumentException> {
            AesGcm.encrypt("data", "short".toByteArray(Charsets.UTF_8))
        }
    }

    @Test
    fun `AES decrypt wrong key length throws`() {
        assertThrows<IllegalArgumentException> {
            AesGcm.decrypt("{}", "short".toByteArray(Charsets.UTF_8))
        }
    }

    // ===== Signer =====

    @Test
    fun `createSignature returns 64 char hex string`() {
        val sig = Signer.createSignature(
            method = "POST",
            pathAndQuery = "/v5/classes/users",
            body = """{"name":"Alice"}""",
            appId = 1001,
            timestamp = 1700000000L,
            secretKeyBytes = "a".repeat(32).toByteArray(Charsets.UTF_8)
        )
        assertEquals(64, sig.length)
    }

    @Test
    fun `createSignature is deterministic`() {
        val keyBytes = "a".repeat(32).toByteArray(Charsets.UTF_8)
        val a = Signer.createSignature("POST", "/v5/classes/users", """{"name":"Alice"}""", 1001, 1700000000L, keyBytes)
        val b = Signer.createSignature("POST", "/v5/classes/users", """{"name":"Alice"}""", 1001, 1700000000L, keyBytes)
        assertEquals(a, b)
    }

    @Test
    fun `createSignature different methods produce different signatures`() {
        val keyBytes = "a".repeat(32).toByteArray(Charsets.UTF_8)
        val a = Signer.createSignature("GET", "/v5/classes/users", "", 1001, 1700000000L, keyBytes)
        val b = Signer.createSignature("POST", "/v5/classes/users", "", 1001, 1700000000L, keyBytes)
        assertNotEquals(a, b)
    }

    @Test
    fun `getSafeTimestamp returns current Unix timestamp with offset`() {
        val ts = Signer.getSafeTimestamp(0)
        val now = System.currentTimeMillis() / 1000
        // Should be within 2 seconds of now
        assertTrue(ts.toLong() in (now - 2)..(now + 2))
    }

    @Test
    fun `getSafeTimestamp applies offset correctly`() {
        val tsNoOffset = Signer.getSafeTimestamp(0).toLong()
        val tsWithOffset = Signer.getSafeTimestamp(100).toLong()
        assertEquals(100, tsWithOffset - tsNoOffset)
    }

    @Test
    fun `sha256RawBytes works on byte arrays`() {
        val result = Hash.sha256RawBytes(byteArrayOf(1, 2, 3))
        assertEquals(64, result.length)
    }
}
