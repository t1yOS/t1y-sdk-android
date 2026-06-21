package com.t1y.sdk

import com.t1y.sdk.special.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class SpecialTypesTest {

    // ===== ObjectID =====

    @Test
    fun `ObjectID creates correct marker`() {
        assertEquals(
            "ObjectID('507f1f77bcf86cd799439011')",
            ObjectID("507f1f77bcf86cd799439011")
        )
    }

    @Test
    fun `ObjectID throws on invalid hex`() {
        assertFailsWith<IllegalArgumentException> {
            ObjectID("zzzzzzzzzzzzzzzzzzzzzzzz")
        }
    }

    @Test
    fun `ObjectID throws on wrong length`() {
        assertFailsWith<IllegalArgumentException> {
            ObjectID("abc")
        }
    }

    // ===== Date Types =====

    @Test
    fun `Date creates correct marker`() {
        assertEquals("Date('2024-01-15T10:30:00Z')", Date("2024-01-15T10:30:00Z"))
    }

    @Test
    fun `DateTime creates correct marker`() {
        assertEquals("DateTime('2024-01-15T10:30:00Z')", DateTime("2024-01-15T10:30:00Z"))
    }

    @Test
    fun `Timestamp from Long creates correct marker`() {
        assertEquals("Timestamp('1705312200')", Timestamp(1705312200L))
    }

    @Test
    fun `Timestamp from String creates correct marker`() {
        assertEquals("Timestamp('1705312200')", Timestamp("1705312200"))
    }

    // ===== Numeric Types =====

    @Test
    fun `Boolean creates correct marker`() {
        assertEquals("Boolean(true)", Boolean(true))
        assertEquals("Boolean(false)", Boolean(false))
    }

    @Test
    fun `Integer creates correct marker`() {
        assertEquals("Integer(42)", Integer(42))
        assertEquals("Integer(-7)", Integer(-7))
    }

    @Test
    fun `Bigint creates correct marker`() {
        assertEquals("Bigint(9007199254740991)", Bigint(9007199254740991L))
    }

    @Test
    fun `Float creates correct marker`() {
        assertEquals("Float(3.14)", Float(3.14))
    }

    @Test
    fun `Double creates correct marker`() {
        assertEquals("Double(3.141592653589793)", Double(3.141592653589793))
    }

    // ===== Structured Types =====

    @Test
    fun `Array creates correct format`() {
        val result = Array(listOf(1, 2, 3))
        assertTrue(result.startsWith("Array("))
        assertTrue(result.endsWith(")"))
    }

    @Test
    fun `Map creates correct format`() {
        val result = Map(mapOf("key" to "value"))
        assertTrue(result.startsWith("Map("))
        assertTrue(result.endsWith(")"))
    }

    @Test
    fun `MapArray creates correct format`() {
        val result = MapArray(listOf(mapOf("a" to 1)))
        assertTrue(result.startsWith("Map[]("))
        assertTrue(result.endsWith(")"))
    }

    // ===== Null Markers =====

    @Test
    fun `Null constants have correct values`() {
        assertEquals("Null", Null)
        assertEquals("None", None)
        assertEquals("Nil", Nil)
        assertEquals("", Empty)
        assertEquals("UNDEFINED", UNDEFINED)
        assertEquals("Undefined", Undefined)
    }

    // ===== Time Helpers =====

    @Test
    fun `TIME_NOW has correct value`() {
        assertEquals("time.Now()", TIME_NOW)
    }

    @Test
    fun `TIME_NOW_UNIX has correct value`() {
        assertEquals("time.Now().Unix()", TIME_NOW_UNIX)
    }

    @Test
    fun `TIME_NOW_UNIX_NANO has correct value`() {
        assertEquals("time.Now().UnixNano()", TIME_NOW_UNIX_NANO)
    }

    @Test
    fun `TIME_NOW_WEEKDAY has correct value`() {
        assertEquals("time.Now().Weekday()", TIME_NOW_WEEKDAY)
    }

    @Test
    fun `TIME_NOW_WEEKDAY_CHINESE has correct value`() {
        assertEquals("time.Now().Weekday().Chinese()", TIME_NOW_WEEKDAY_CHINESE)
    }

    // ===== timeNow object =====

    @Test
    fun `timeNow Now() returns correct value`() {
        assertEquals("time.Now()", timeNow.Now())
    }

    @Test
    fun `timeNow NowUnix() returns correct value`() {
        assertEquals("time.Now().Unix()", timeNow.NowUnix())
    }

    @Test
    fun `timeNow NowUnixNano() returns correct value`() {
        assertEquals("time.Now().UnixNano()", timeNow.NowUnixNano())
    }

    @Test
    fun `timeNow NowWeekday() returns correct value`() {
        assertEquals("time.Now().Weekday()", timeNow.NowWeekday())
    }

    @Test
    fun `timeNow NowWeekdayChinese() returns correct value`() {
        assertEquals("time.Now().Weekday().Chinese()", timeNow.NowWeekdayChinese())
    }
}
