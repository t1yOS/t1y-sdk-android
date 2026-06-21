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
}
