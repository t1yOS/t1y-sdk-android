package com.t1y.sdk

import com.t1y.sdk.exception.ValidationException
import com.t1y.sdk.util.TypeChecks
import com.t1y.sdk.util.Validation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class ValidationTest {

    // ===== Config Validation =====

    @Test
    fun `valid config does not throw`() {
        assertDoesNotThrow {
            Config(
                baseUrl = "https://myapp.t1y.net",
                appId = 1001,
                apiKey = "a".repeat(32),
                secretKey = "b".repeat(32)
            )
        }
    }

    @Test
    fun `config with custom baseUrl`() {
        assertDoesNotThrow {
            Config(
                baseUrl = "http://localhost:3000",
                appId = 2000,
                apiKey = "a".repeat(32),
                secretKey = "b".repeat(32)
            )
        }
    }

    @Test
    fun `appId less than 1001 throws`() {
        val ex = assertThrows<IllegalArgumentException> {
            Config(
                appId = 500,
                apiKey = "a".repeat(32),
                secretKey = "b".repeat(32)
            )
        }
        assertTrue(ex.message!!.contains("1001"))
    }

    @Test
    fun `apiKey wrong length throws`() {
        val ex = assertThrows<IllegalArgumentException> {
            Config(
                appId = 1001,
                apiKey = "short",
                secretKey = "b".repeat(32)
            )
        }
        assertTrue(ex.message!!.contains("32"))
    }

    @Test
    fun `secretKey wrong length throws`() {
        val ex = assertThrows<IllegalArgumentException> {
            Config(
                appId = 1001,
                apiKey = "a".repeat(32),
                secretKey = "short"
            )
        }
        assertTrue(ex.message!!.contains("32"))
    }

    @Test
    fun `invalid baseUrl throws`() {
        val ex = assertThrows<IllegalArgumentException> {
            Config(
                baseUrl = "ftp://invalid.com",
                appId = 1001,
                apiKey = "a".repeat(32),
                secretKey = "b".repeat(32)
            )
        }
        assertTrue(ex.message!!.contains("http"))
    }

    @Test
    fun `negative version throws`() {
        val ex = assertThrows<IllegalArgumentException> {
            Config(
                appId = 1001,
                apiKey = "a".repeat(32),
                secretKey = "b".repeat(32),
                version = -1
            )
        }
        assertTrue(ex.message!!.contains("version"))
    }

    @Test
    fun `default values are set correctly`() {
        val config = Config(
            appId = 1001,
            apiKey = "a".repeat(32),
            secretKey = "b".repeat(32)
        )
        assertEquals(Constants.DEFAULT_BASE_URL, config.baseUrl)
        assertEquals(Constants.DEFAULT_VERSION, config.version)
        assertEquals(Constants.DEFAULT_SAFE_MODE, config.isSafeMode)
        assertEquals(Constants.DEFAULT_OFFSET, config.offset)
    }

    // ===== ObjectID Validation =====

    @Test
    fun `valid ObjectID passes validation`() {
        assertTrue(Validation.assertObjectID("507f1f77bcf86cd799439011"))
    }

    @Test
    fun `valid ObjectID with mixed case passes`() {
        assertTrue(Validation.assertObjectID("507f1f77bCF86cd799439011"))
    }

    @Test
    fun `ObjectID wrong length throws`() {
        assertThrows<IllegalArgumentException> {
            Validation.assertObjectID("abc")
        }
    }

    @Test
    fun `ObjectID non-hex characters throw`() {
        assertThrows<IllegalArgumentException> {
            Validation.assertObjectID("gggggggggggggggggggggggg")
        }
    }

    @Test
    fun `ObjectID with custom name in error message`() {
        val ex = assertThrows<IllegalArgumentException> {
            Validation.assertObjectID("bad", "CustomID")
        }
        assertTrue(ex.message!!.contains("CustomID"))
    }

    // ===== requireNonEmptyObject =====

    @Test
    fun `non-empty map passes requireNonEmptyObject`() {
        assertDoesNotThrow {
            Validation.requireNonEmptyObject(mapOf("key" to "value"))
        }
    }

    @Test
    fun `null fails requireNonEmptyObject`() {
        val ex = assertThrows<ValidationException> {
            Validation.requireNonEmptyObject(null)
        }
        assertTrue(ex.message!!.contains("null"))
    }

    @Test
    fun `empty map fails requireNonEmptyObject`() {
        val ex = assertThrows<ValidationException> {
            Validation.requireNonEmptyObject(emptyMap<String, Any>())
        }
        assertTrue(ex.message!!.contains("empty"))
    }

    @Test
    fun `list fails requireNonEmptyObject`() {
        val ex = assertThrows<ValidationException> {
            Validation.requireNonEmptyObject(listOf(1, 2, 3))
        }
        assertTrue(ex.message!!.contains("List"))
    }

    // ===== requirePlainObject =====

    @Test
    fun `map passes requirePlainObject`() {
        assertDoesNotThrow {
            Validation.requirePlainObject(emptyMap<String, Any>())
        }
    }

    @Test
    fun `null fails requirePlainObject`() {
        assertThrows<ValidationException> {
            Validation.requirePlainObject(null)
        }
    }

    @Test
    fun `list fails requirePlainObject`() {
        assertThrows<ValidationException> {
            Validation.requirePlainObject(listOf(1, 2, 3))
        }
    }

    // ===== requireNonEmptyArrayWithNonEmptyObjects =====

    @Test
    fun `valid array passes`() {
        assertDoesNotThrow {
            Validation.requireNonEmptyArrayWithNonEmptyObjects(
                listOf(mapOf("a" to 1), mapOf("b" to 2))
            )
        }
    }

    @Test
    fun `empty list fails`() {
        assertThrows<ValidationException> {
            Validation.requireNonEmptyArrayWithNonEmptyObjects(emptyList<Any>())
        }
    }

    @Test
    fun `list with empty map fails`() {
        assertThrows<ValidationException> {
            Validation.requireNonEmptyArrayWithNonEmptyObjects(listOf(emptyMap<String, Any>()))
        }
    }

    @Test
    fun `list with non-map element fails`() {
        assertThrows<ValidationException> {
            Validation.requireNonEmptyArrayWithNonEmptyObjects(listOf("not a map"))
        }
    }

    @Test
    fun `null fails requireNonEmptyArrayWithNonEmptyObjects`() {
        assertThrows<ValidationException> {
            Validation.requireNonEmptyArrayWithNonEmptyObjects(null)
        }
    }

    // ===== Type Checks =====

    @Test
    fun `isNonEmptyObject with valid map`() {
        assertTrue(TypeChecks.isNonEmptyObject(mapOf("a" to 1)))
    }

    @Test
    fun `isNonEmptyObject with empty map`() {
        assertFalse(TypeChecks.isNonEmptyObject(emptyMap<String, Any>()))
    }

    @Test
    fun `isNonEmptyObject with null`() {
        assertFalse(TypeChecks.isNonEmptyObject(null))
    }

    @Test
    fun `isNonEmptyObject with list`() {
        assertFalse(TypeChecks.isNonEmptyObject(listOf(1, 2)))
    }

    @Test
    fun `isPlainObject with map`() {
        assertTrue(TypeChecks.isPlainObject(emptyMap<String, Any>()))
    }

    @Test
    fun `isPlainObject with null`() {
        assertFalse(TypeChecks.isPlainObject(null))
    }

    @Test
    fun `isPlainObject with string`() {
        assertFalse(TypeChecks.isPlainObject("hello"))
    }

    @Test
    fun `isNonEmptyArrayWithNonEmptyObjects valid`() {
        assertTrue(
            TypeChecks.isNonEmptyArrayWithNonEmptyObjects(
                listOf(mapOf("a" to 1))
            )
        )
    }

    @Test
    fun `isNonEmptyArrayWithNonEmptyObjects with empty list`() {
        assertFalse(TypeChecks.isNonEmptyArrayWithNonEmptyObjects(emptyList<Any>()))
    }

    @Test
    fun `isNonEmptyArrayWithNonEmptyObjects with null`() {
        assertFalse(TypeChecks.isNonEmptyArrayWithNonEmptyObjects(null))
    }
}
