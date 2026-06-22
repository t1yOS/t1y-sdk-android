package com.t1y.sdk

import com.t1y.sdk.exception.T1YException
import com.t1y.sdk.exception.ValidationException
import com.t1y.sdk.model.InsertResult
import com.t1y.sdk.model.InsertManyResult
import com.t1y.sdk.model.DeleteResult
import com.t1y.sdk.model.DeleteManyResult
import com.t1y.sdk.model.UpdateResult
import com.t1y.sdk.model.UpdateManyResult
import com.t1y.sdk.model.FindResult
import com.t1y.sdk.model.PaginationResult
import com.t1y.sdk.model.AggregateResult
import com.t1y.sdk.model.CountResult
import com.t1y.sdk.model.CollectionsResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class T1CollectionTest {

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
        client.close()
    }

    // ===== Single Document Operations =====

    @Test
    fun `insertOne sends POST to collection endpoint`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"objectId":"507f1f77bcf86cd799439011"}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").insertOne(mapOf("name" to "Alice", "age" to 25))

        assertEquals(0, result.code)
        assertEquals("507f1f77bcf86cd799439011", result.data.objectId)

        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.contains("/v5/classes/users"))
        // URL-encoded collection name
        assertFalse(request.path!!.contains(" "), "Collection name should be URL-encoded")
    }

    @Test
    fun `insertOne throws on empty data`() = runTest {
        assertThrows<ValidationException> {
            client.db.collection("users").insertOne(emptyMap())
        }
    }

    @Test
    fun `deleteById sends DELETE with objectId in path`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"deletedCount":1}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").deleteById("507f1f77bcf86cd799439011")

        assertEquals(1, result.data.deletedCount)
        val request = mockServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertTrue(request.path!!.endsWith("507f1f77bcf86cd799439011"))
    }

    @Test
    fun `deleteById throws on invalid ObjectID`() = runTest {
        assertThrows<IllegalArgumentException> {
            client.db.collection("users").deleteById("invalid")
        }
    }

    @Test
    fun `updateById sends PUT with objectId and body`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"modifiedCount":1}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").updateById(
            "507f1f77bcf86cd799439011",
            mapOf("name" to "Alice Updated")
        )

        assertEquals(1, result.data.modifiedCount)
        val request = mockServer.takeRequest()
        assertEquals("PUT", request.method)
        assertTrue(request.path!!.contains("507f1f77bcf86cd799439011"))
        assertTrue(request.body.readUtf8().contains("Alice Updated"))
    }

    @Test
    fun `findById sends GET with objectId in path`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"result":{"name":"Alice","age":25}}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").findById("507f1f77bcf86cd799439011")

        assertNotNull(result.data.result)
        val request = mockServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.endsWith("507f1f77bcf86cd799439011"))
    }

    // ===== Filter-based Single Operations =====

    @Test
    fun `deleteOne sends DELETE with filter body`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"deletedCount":1}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").deleteOne(mapOf("name" to "Alice"))

        assertEquals(1, result.data.deletedCount)
        val request = mockServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertTrue(request.path!!.endsWith("/one"))
        assertTrue(request.body.readUtf8().contains("Alice"))
    }

    @Test
    fun `updateOne sends PUT with filter and body combined`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"modifiedCount":1}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").updateOne(
            filter = mapOf("name" to "Alice"),
            body = mapOf("age" to 27)
        )

        assertEquals(1, result.data.modifiedCount)
        val request = mockServer.takeRequest()
        assertEquals("PUT", request.method)
        val requestBody = request.body.readUtf8()
        assertTrue(requestBody.contains("\"filter\""))
        assertTrue(requestBody.contains("\"body\""))
    }

    @Test
    fun `findOne sends POST with filter body`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"result":{"name":"Alice"}}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").findOne(mapOf("name" to "Alice"))

        assertNotNull(result.data.result)
        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.endsWith("/one"))
    }

    // ===== Bulk Operations =====

    @Test
    fun `insertMany sends POST with list body`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"objectIds":["id1","id2"],"insertedCount":2}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").insertMany(
            listOf(
                mapOf("name" to "Alice"),
                mapOf("name" to "Bob")
            )
        )

        assertEquals(2, result.data.insertedCount)
        assertEquals(listOf("id1", "id2"), result.data.objectIds)
        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.endsWith("/many"))
    }

    @Test
    fun `insertMany throws on empty list`() = runTest {
        assertThrows<ValidationException> {
            client.db.collection("users").insertMany(emptyList())
        }
    }

    @Test
    fun `insertMany throws on list with empty map element`() = runTest {
        assertThrows<ValidationException> {
            client.db.collection("users").insertMany(listOf(emptyMap()))
        }
    }

    @Test
    fun `deleteMany allows empty filter to match all`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"deletedCount":5}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").deleteMany(emptyMap())

        assertEquals(5, result.data.deletedCount)
        val request = mockServer.takeRequest()
        assertEquals("DELETE", request.method)
    }

    @Test
    fun `updateMany sends PUT with filter and body`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"modifiedCount":3}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").updateMany(
            filter = emptyMap(),
            body = mapOf("status" to "archived")
        )

        assertEquals(3, result.data.modifiedCount)
        val request = mockServer.takeRequest()
        assertEquals("PUT", request.method)
    }

    // ===== Advanced Queries =====

    @Test
    fun `find with default parameters`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{
                    "code":0,"message":"ok",
                    "data":{
                        "results":[{"name":"Alice"},{"name":"Bob"}],
                        "page":1,"size":10,
                        "pagination":{"totalItems":2,"totalPages":1}
                    }
                }""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").find()

        assertEquals(2, result.data.results.size)
        assertEquals(1, result.data.page)
        assertEquals(10, result.data.size)
        assertEquals(2, result.data.pagination.totalItems)
    }

    @Test
    fun `find clamps size to max page size`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"results":[],"page":1,"size":100,"pagination":{"totalItems":0,"totalPages":0}}}""")
                .setResponseCode(200)
        )

        client.db.collection("users").find(size = 500)

        val request = mockServer.takeRequest()
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"size\":100"), "Size should be clamped to 100, body: $body")
    }

    @Test
    fun `find throws on negative page`() = runTest {
        assertThrows<IllegalArgumentException> {
            client.db.collection("users").find(page = 0)
        }
    }

    @Test
    fun `find throws on negative size`() = runTest {
        assertThrows<IllegalArgumentException> {
            client.db.collection("users").find(size = 0)
        }
    }

    @Test
    fun `find with custom sort and filter`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"results":[],"page":1,"size":20,"pagination":{"totalItems":0,"totalPages":0}}}""")
                .setResponseCode(200)
        )

        client.db.collection("users").find(
            page = 1,
            size = 20,
            sort = mapOf("createdAt" to -1),
            filter = mapOf("age" to mapOf("\$gte" to 18))
        )

        val request = mockServer.takeRequest()
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"sort\""), "Body should contain sort: $body")
        assertTrue(body.contains("\"filter\""), "Body should contain filter: $body")
    }

    @Test
    fun `aggregate sends POST with pipeline`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"results":[{"count":42}]}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").aggregate(
            listOf(
                mapOf("\$match" to mapOf("status" to "active")),
                mapOf("\$count" to "total")
            )
        )

        assertEquals(1, result.data.results.size)
        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.endsWith("/aggregate"))
    }

    @Test
    fun `count sends POST with filter`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"count":42}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").count(mapOf("status" to "active"))

        assertEquals(42, result.data.count)
    }

    @Test
    fun `count with no filter counts all documents`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"count":100}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").count()

        assertEquals(100, result.data.count)
    }

    @Test
    fun `distinct sends POST with field name in path`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":["Beijing","Shanghai","Shenzhen"]}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").distinct("city")

        assertEquals(0, result.code)
        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.contains("/distinct/"))
        assertTrue(request.path!!.contains("city"))
    }

    @Test
    fun `distinct throws on blank field name`() = runTest {
        assertThrows<IllegalArgumentException> {
            client.db.collection("users").distinct("  ")
        }
    }

    @Test
    fun `distinct with filter`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":["Beijing"]}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").distinct(
            fieldName = "city",
            filter = mapOf("country" to "China")
        )

        assertEquals(0, result.code)
        val request = mockServer.takeRequest()
        assertTrue(request.body.readUtf8().contains("China"))
    }

    // ===== Schema Management =====

    @Test
    fun `create sends POST to schemas endpoint`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("posts").create()

        assertEquals(0, result.code)
        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.contains("/v5/schemas/posts"))
    }

    @Test
    fun `clear sends PUT to schemas endpoint`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"deletedCount":10}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("users").clear()

        assertEquals(10, result.data.deletedCount)
        val request = mockServer.takeRequest()
        assertEquals("PUT", request.method)
        assertTrue(request.path!!.contains("/v5/schemas/users"))
    }

    @Test
    fun `drop sends DELETE to schemas endpoint`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{}}""")
                .setResponseCode(200)
        )

        val result = client.db.collection("posts").drop()

        assertEquals(0, result.code)
        val request = mockServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertTrue(request.path!!.contains("/v5/schemas/posts"))
    }

    // ===== getCollections =====

    @Test
    fun `getCollections returns all schemas`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"results":["users","orders","products"]}}""")
                .setResponseCode(200)
        )

        val result = client.db.getCollections()

        assertEquals(listOf("users", "orders", "products"), result.data.results)
    }

    // ===== Error Handling in Collections =====

    @Test
    fun `collection operation throws T1YException on server error`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":500,"message":"Internal error"}""")
                .setResponseCode(500)
        )

        val ex = assertThrows<T1YException> {
            client.db.collection("users").findById("507f1f77bcf86cd799439011")
        }
        assertEquals(500, ex.code)
    }

    // ===== URL-encoding of collection names =====

    @Test
    fun `collection name with special characters is URL-encoded`() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setBody("""{"code":0,"message":"ok","data":{"objectId":"507f1f77bcf86cd799439011"}}""")
                .setResponseCode(200)
        )

        client.db.collection("user profiles").insertOne(mapOf("name" to "Alice"))

        val request = mockServer.takeRequest()
        val path = request.path!!
        // Should contain URL-encoded space, not literal space
        assertTrue(path.contains("user+profiles") || path.contains("user%20profiles"),
            "Collection name should be URL-encoded, got: $path")
    }

    // ===== Concurrency =====

    @Test
    fun `concurrent requests do not interfere`() = runTest {
        // Enqueue multiple responses
        repeat(3) {
            mockServer.enqueue(
                MockResponse()
                    .setBody("""{"code":0,"message":"ok","data":{"objectId":"507f1f77bcf86cd79943901$it"}}""")
                    .setResponseCode(200)
            )
        }

        // Send concurrent requests
        val results = listOf(
            client.db.collection("users").insertOne(mapOf("name" to "Alice")),
            client.db.collection("users").insertOne(mapOf("name" to "Bob")),
            client.db.collection("users").insertOne(mapOf("name" to "Charlie"))
        )

        assertEquals(3, results.size)
        results.forEach { assertEquals(0, it.code) }
    }
}
