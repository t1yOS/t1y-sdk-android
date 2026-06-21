package com.t1y.sdk

import com.t1y.sdk.model.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class ApiResponseTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `InsertResult deserialization`() {
        val jsonStr = """{"code":0,"message":"ok","data":{"objectId":"507f1f77bcf86cd799439011"}}"""
        val response = json.decodeFromString<ApiResponse<InsertResult>>(jsonStr)
        assertEquals(0, response.code)
        assertEquals("ok", response.message)
        assertEquals("507f1f77bcf86cd799439011", response.data.objectId)
    }

    @Test
    fun `InsertManyResult deserialization`() {
        val jsonStr = """{"code":0,"message":"ok","data":{"objectIds":["id1","id2"],"insertedCount":2}}"""
        val response = json.decodeFromString<ApiResponse<InsertManyResult>>(jsonStr)
        assertEquals(2, response.data.insertedCount)
        assertEquals(listOf("id1", "id2"), response.data.objectIds)
    }

    @Test
    fun `DeleteResult deserialization`() {
        val jsonStr = """{"code":0,"message":"ok","data":{"deletedCount":1}}"""
        val response = json.decodeFromString<ApiResponse<DeleteResult>>(jsonStr)
        assertEquals(1, response.data.deletedCount)
    }

    @Test
    fun `UpdateResult deserialization`() {
        val jsonStr = """{"code":0,"message":"ok","data":{"modifiedCount":1}}"""
        val response = json.decodeFromString<ApiResponse<UpdateResult>>(jsonStr)
        assertEquals(1, response.data.modifiedCount)
    }

    @Test
    fun `PaginationResult deserialization`() {
        val jsonStr = """
        {
            "code": 0,
            "message": "ok",
            "data": {
                "results": [{"name": "Alice"}, {"name": "Bob"}],
                "page": 1,
                "size": 10,
                "pagination": {"totalItems": 2, "totalPages": 1}
            }
        }
        """.trimIndent()
        val response = json.decodeFromString<ApiResponse<PaginationResult>>(jsonStr)
        assertEquals(1, response.data.page)
        assertEquals(10, response.data.size)
        assertEquals(2, response.data.results.size)
        assertEquals(2, response.data.pagination.totalItems)
    }

    @Test
    fun `AggregateResult deserialization`() {
        val jsonStr = """
        {
            "code": 0,
            "message": "ok",
            "data": {
                "results": [{"count": 42}]
            }
        }
        """.trimIndent()
        val response = json.decodeFromString<ApiResponse<AggregateResult>>(jsonStr)
        assertEquals(1, response.data.results.size)
    }

    @Test
    fun `InitResult deserialization`() {
        val jsonStr = """{"code":0,"message":"ok","data":{"unix":1700000000,"is_safe_mode":true}}"""
        val response = json.decodeFromString<ApiResponse<InitResult>>(jsonStr)
        assertEquals(1700000000L, response.data.unix)
        assertTrue(response.data.isSafeMode)
    }

    @Test
    fun `CountResult deserialization`() {
        val jsonStr = """{"code":0,"message":"ok","data":{"count":42}}"""
        val response = json.decodeFromString<ApiResponse<CountResult>>(jsonStr)
        assertEquals(42, response.data.count)
    }

    @Test
    fun `CollectionsResult deserialization`() {
        val jsonStr = """{"code":0,"message":"ok","data":{"results":["users","posts","comments"]}}"""
        val response = json.decodeFromString<ApiResponse<CollectionsResult>>(jsonStr)
        assertEquals(listOf("users", "posts", "comments"), response.data.results)
    }

    @Test
    fun `DynamicJson toAny converts JsonNull to null`() {
        assertNull(JsonNull.toAny())
    }

    @Test
    fun `DynamicJson toAny converts JsonPrimitive string`() {
        assertEquals("hello", JsonPrimitive("hello").toAny())
    }

    @Test
    fun `DynamicJson toAny converts JsonPrimitive number`() {
        assertEquals(42L, JsonPrimitive(42).toAny())
    }

    @Test
    fun `DynamicJson toAny converts JsonPrimitive boolean`() {
        assertEquals(true, JsonPrimitive(true).toAny())
    }

    @Test
    fun `DynamicJson toMap on JsonObject`() {
        val obj = buildJsonObject {
            put("name", JsonPrimitive("Alice"))
            put("age", JsonPrimitive(25))
        }
        val map = obj.toMap()
        assertEquals("Alice", map["name"])
        assertEquals(25L, map["age"])
    }

    @Test
    fun `DynamicJson toList on JsonArray`() {
        val arr = buildJsonArray {
            add(JsonPrimitive("a"))
            add(JsonPrimitive("b"))
        }
        val list = arr.toList()
        assertEquals(listOf("a", "b"), list)
    }

    @Test
    fun `DynamicJson toJsonElement converts Map`() {
        val map = mapOf("name" to "Alice", "age" to 25)
        val element = map.toJsonElement()
        assertTrue(element is JsonObject)
        val obj = element as JsonObject
        assertEquals("Alice", (obj["name"] as JsonPrimitive).content)
    }

    @Test
    fun `DynamicJson toJsonElement converts List`() {
        val list = listOf("a", "b", "c")
        val element = list.toJsonElement()
        assertTrue(element is JsonArray)
        assertEquals(3, (element as JsonArray).size)
    }

    @Test
    fun `DynamicJson toJsonElement converts null`() {
        assertTrue((null as Any?).toJsonElement() is JsonNull)
    }

    @Test
    fun `T1YException construction and properties`() {
        val ex = com.t1y.sdk.exception.T1YException(404, "Not found", """{"error":"missing"}""")
        assertEquals(404, ex.code)
        assertEquals("Not found", ex.message)
        assertEquals("""{"error":"missing"}""", ex.data)
    }

    @Test
    fun `ValidationException construction`() {
        val ex = com.t1y.sdk.exception.ValidationException("Invalid input")
        assertEquals("Invalid input", ex.message)
        assertTrue(ex is IllegalArgumentException)
    }
}
