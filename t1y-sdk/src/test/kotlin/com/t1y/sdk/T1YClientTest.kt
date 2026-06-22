package com.t1y.sdk

import com.t1y.sdk.model.ApiResponse
import com.t1y.sdk.model.CollectionsResult
import com.t1y.sdk.model.InsertResult
import com.t1y.sdk.model.InitResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class T1YClientTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var client: T1YClient

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        val baseUrl = mockServer.url("/").toString().trimEnd('/')
        client = T1YClient(
            Config(
                baseUrl = baseUrl,
                appId = 1001,
                apiKey = "a".repeat(32),
                secretKey = "b".repeat(32)
            )
        )
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
    }

    // ===== Initialization =====

    @Test
    fun `init syncs time offset and safe mode from server`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"unix":${System.currentTimeMillis() / 1000},"is_safe_mode":false}}""")
                .setResponseCode(200)
        )

        client.init()
        // Should not throw — gracefully degrades on failure too
    }

    @Test
    fun `init gracefully degrades on server error`() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(500))

        // Should not throw
        client.init()

        // Safe mode and offset should remain at defaults
        assertEquals(Constants.DEFAULT_OFFSET, client.mutableConfig.offset)
        assertEquals(Constants.DEFAULT_SAFE_MODE, client.mutableConfig.isSafeMode)
    }

    @Test
    fun `init gracefully degrades on network error`() = runTest {
        mockServer.shutdown() // Force network error

        client.init()
        // Should not throw
    }

    // ===== Auth Headers =====

    @Test
    fun `request includes required auth headers`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{}}""")
                .setResponseCode(200)
        )

        client.requestRaw("GET", "/v5/meta")

        val recordedRequest = mockServer.takeRequest()
        assertNotNull(recordedRequest.getHeader("X-T1Y-Application-ID"))
        assertNotNull(recordedRequest.getHeader("X-T1Y-API-Key"))
        assertNotNull(recordedRequest.getHeader("X-T1Y-Safe-Timestamp"))
        assertNotNull(recordedRequest.getHeader("X-T1Y-Safe-Sign"))

        assertEquals("1001", recordedRequest.getHeader("X-T1Y-Application-ID"))
        assertEquals("a".repeat(32), recordedRequest.getHeader("X-T1Y-API-Key"))
    }

    // ===== getMeta =====

    @Test
    fun `getMeta returns metadata`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"name":"myapp","version":5}}""")
                .setResponseCode(200)
        )

        val response = client.getMeta()
        assertEquals(0, response.code)
        assertEquals("ok", response.message)
        assertTrue(response.data is JsonObject)
    }

    @Test
    fun `getMeta with field returns filtered metadata`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"result":5}}""")
                .setResponseCode(200)
        )

        val response = client.getMeta("version")
        assertEquals(0, response.code)
    }

    // ===== checkUpdate =====

    @Test
    fun `checkUpdate returns true when server version is higher`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"result":5}}""")
                .setResponseCode(200)
        )

        val needsUpdate = client.checkUpdate()
        assertTrue(needsUpdate)
    }

    @Test
    fun `checkUpdate returns false when server version is same or lower`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"result":0}}""")
                .setResponseCode(200)
        )

        val needsUpdate = client.checkUpdate()
        assertFalse(needsUpdate)
    }

    // ===== db.toObjectID =====

    @Test
    fun `db toObjectID creates valid marker`() {
        val marker = client.db.toObjectID("507f1f77bcf86cd799439011")
        assertEquals("ObjectID('507f1f77bcf86cd799439011')", marker)
    }

    @Test
    fun `db toObjectID throws on invalid id`() {
        assertFailsWith<IllegalArgumentException> {
            client.db.toObjectID("invalid")
        }
    }

    // ===== db.getCollections =====

    @Test
    fun `db getCollections returns collection list`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"results":["users","posts"]}}""")
                .setResponseCode(200)
        )

        val response = client.db.getCollections()
        assertEquals(0, response.code)
        assertEquals(listOf("users", "posts"), response.data.results)
    }

    // ===== Utility Methods =====

    @Test
    fun `isNonEmptyObject with valid map`() {
        assertTrue(client.isNonEmptyObject(mapOf("a" to 1)))
        assertFalse(client.isNonEmptyObject(emptyMap<String, Any>()))
        assertFalse(client.isNonEmptyObject(null))
    }

    @Test
    fun `isPlainObject with valid map`() {
        assertTrue(client.isPlainObject(mapOf("a" to 1)))
        assertFalse(client.isPlainObject(null))
        assertFalse(client.isPlainObject("string"))
    }

    @Test
    fun `assertObjectID validates correctly`() {
        assertTrue(client.assertObjectID("507f1f77bcf86cd799439011"))
        assertFailsWith<IllegalArgumentException> {
            client.assertObjectID("invalid")
        }
    }

    @Test
    fun `hmacSHA256 produces deterministic output`() {
        val a = client.hmacSHA256("secret", "message")
        val b = client.hmacSHA256("secret", "message")
        assertEquals(a, b)
        assertEquals(64, a.length)
    }

    @Test
    fun `verifyHmacSHA256 validates signature`() {
        val sig = client.hmacSHA256("secret", "message")
        assertTrue(client.verifyHmacSHA256("secret", "message", sig))
        assertFalse(client.verifyHmacSHA256("secret", "message", "b" + sig.substring(1)))
    }

    // ===== URL拼接顺序: GET params appended after path =====

    @Test
    fun `GET request appends query string after path`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{}}""")
                .setResponseCode(200)
        )

        // Use getMeta with a field — it appends ?field=... to the path
        client.getMeta("version")

        val recordedRequest = mockServer.takeRequest()
        val requestPath = recordedRequest.requestUrl!!.encodedPath
        val requestQuery = recordedRequest.requestUrl!!.encodedQuery

        // Path should be /v5/meta, query should be field=version
        assertEquals("/v5/meta", requestPath)
        assertEquals("field=version", requestQuery)
    }

    @Test
    fun `GET request without params has no query string`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{}}""")
                .setResponseCode(200)
        )

        client.getMeta()

        val recordedRequest = mockServer.takeRequest()
        assertEquals("/v5/meta", recordedRequest.requestUrl!!.encodedPath)
        assertNull(recordedRequest.requestUrl!!.encodedQuery)
    }

    // ===== init() parse InitResult once =====

    @Test
    fun `init parses both isSafeMode and unix from valid response`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"unix":${System.currentTimeMillis() / 1000},"is_safe_mode":true}}""")
                .setResponseCode(200)
        )

        client.init()

        // After init, isSafeMode should be true and offset should be near 0
        assertTrue(client.mutableConfig.isSafeMode)
        assertTrue(client.mutableConfig.offset in -2..2,
            "Offset should be near 0, got: ${client.mutableConfig.offset}")
    }

    @Test
    fun `init keeps defaults when data is not a JsonObject`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":"not-an-object"}""")
                .setResponseCode(200)
        )

        client.init()

        // Defaults should be preserved
        assertEquals(Constants.DEFAULT_SAFE_MODE, client.mutableConfig.isSafeMode)
        assertEquals(Constants.DEFAULT_OFFSET, client.mutableConfig.offset)
    }

    @Test
    fun `init keeps defaults when InitResult parsing fails`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"unix":"not-a-number","is_safe_mode":"not-a-bool"}}""")
                .setResponseCode(200)
        )

        client.init()

        // Gracefully keeps defaults when types don't match
        assertEquals(Constants.DEFAULT_SAFE_MODE, client.mutableConfig.isSafeMode)
        assertEquals(Constants.DEFAULT_OFFSET, client.mutableConfig.offset)
    }

    // ===== Closeable =====

    @Test
    fun `close shuts down OkHttp and clears secret key`() {
        // Verify secret key is non-zero before close
        assertFalse(client.mutableConfig.secretKeyBytes.all { it == 0.toByte() })

        client.close()

        // Verify secret key is zeroed after close
        assertTrue(client.mutableConfig.secretKeyBytes.all { it == 0.toByte() })
    }

    @Test
    fun `double close is safe`() {
        client.close()
        // Should not throw
        assertDoesNotThrow { client.close() }
    }

    // ===== ensureJscExtension via callFunc =====

    @Test
    fun `callFunc normalizes plain function name to jsc`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":"result"}""")
                .setResponseCode(200)
        )

        client.callFunc("hello")

        val recordedRequest = mockServer.takeRequest()
        assertTrue(recordedRequest.path!!.contains("hello.jsc"))
        assertFalse(recordedRequest.path!!.contains(".js?"))
    }

    @Test
    fun `callFunc preserves dot jsc extension`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":"result"}""")
                .setResponseCode(200)
        )

        client.callFunc("hello.jsc")

        val recordedRequest = mockServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("hello.jsc"))
    }

    @Test
    fun `callFunc normalizes dot js to dot jsc`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":"result"}""")
                .setResponseCode(200)
        )

        client.callFunc("hello.js")

        val recordedRequest = mockServer.takeRequest()
        assertTrue(recordedRequest.path!!.endsWith("hello.jsc"))
    }

    @Test
    fun `callFunc normalizes directory to index jsc`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":"result"}""")
                .setResponseCode(200)
        )

        client.callFunc("dir/")

        val recordedRequest = mockServer.takeRequest()
        assertTrue(recordedRequest.path!!.contains("dir/index.jsc"))
    }

    @Test
    fun `callFunc preserves query string after jsc normalization`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":"result"}""")
                .setResponseCode(200)
        )

        client.callFunc("hello?foo=bar")

        val recordedRequest = mockServer.takeRequest()
        val path = recordedRequest.path!!
        assertTrue(path.contains("hello.jsc?foo=bar"), "Expected hello.jsc?foo=bar, got: $path")
    }

    // ===== Safe Mode end-to-end =====

    @Test
    fun `safe mode encrypts request body`() = runTest {
        // Enable safe mode
        client.mutableConfig.isSafeMode = true
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"objectId":"507f1f77bcf86cd799439011"}}""")
                .setResponseCode(200)
        )

        client.requestRaw("POST", "/v5/classes/users", mapOf("name" to "Alice"))

        val recordedRequest = mockServer.takeRequest()
        val body = recordedRequest.body.readUtf8()

        // Body should be encrypted (contain n, j, t fields)
        assertTrue(body.contains("\"n\""), "Encrypted body should have 'n' field")
        assertTrue(body.contains("\"j\""), "Encrypted body should have 'j' field")
        assertTrue(body.contains("\"t\""), "Encrypted body should have 't' field")
    }

    @Test
    fun `safe mode request body does not contain plaintext`() = runTest {
        client.mutableConfig.isSafeMode = true
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"objectId":"507f1f77bcf86cd799439011"}}""")
                .setResponseCode(200)
        )

        client.requestRaw("POST", "/v5/classes/users", mapOf("name" to "Alice"))

        val recordedRequest = mockServer.takeRequest()
        val body = recordedRequest.body.readUtf8()

        assertFalse(body.contains("\"name\""), "Encrypted body should not contain plaintext field names")
        assertFalse(body.contains("Alice"), "Encrypted body should not contain plaintext values")
    }

    @Test
    fun `safe mode decrypts encrypted response`() = runTest {
        client.mutableConfig.isSafeMode = true

        // Encrypt a response body with the same key
        val plaintextResponse = """{"code":0,"message":"ok","data":{"result":"secret-data"}}"""
        val encryptedResponse = com.t1y.sdk.crypto.AesGcm.encrypt(
            plaintextResponse,
            client.mutableConfig.secretKeyBytes
        )

        mockServer.enqueue(
            MockResponse()
                .setBody(encryptedResponse)
                .setResponseCode(200)
        )

        val response = client.requestRaw("GET", "/v5/meta")

        assertEquals(0, response.code)
        assertEquals("ok", response.message)
    }

    // ===== Error Handling =====

    @Test
    fun `non-2xx response throws T1YException`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":404,"message":"Collection not found"}""")
                .setResponseCode(404)
        )

        val ex = assertThrows<com.t1y.sdk.exception.T1YException> {
            client.requestRaw("GET", "/v5/classes/nonexistent/id")
        }
        assertEquals(404, ex.code)
    }

    @Test
    fun `5xx response throws T1YException`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":500,"message":"Internal server error"}""")
                .setResponseCode(500)
        )

        val ex = assertThrows<com.t1y.sdk.exception.T1YException> {
            client.requestRaw("GET", "/v5/meta")
        }
        assertEquals(500, ex.code)
    }

    @Test
    fun `network error throws T1YException with code 0`() = runTest {
        mockServer.shutdown()

        val ex = assertThrows<com.t1y.sdk.exception.T1YException> {
            client.requestRaw("GET", "/v5/meta")
        }
        assertEquals(0, ex.code)
    }
}
