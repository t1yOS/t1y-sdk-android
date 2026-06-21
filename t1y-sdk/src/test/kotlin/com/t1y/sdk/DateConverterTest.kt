package com.t1y.sdk

import com.t1y.sdk.util.convertDateTypes
import org.junit.jupiter.api.Test
import java.util.Date
import kotlin.test.*

class DateConverterTest {

    @Test
    fun `java util Date is converted to Date marker`() {
        val date = Date(1705312200000L)
        val result = date.convertDateTypes()
        assertTrue(result is String)
        assertTrue((result as String).startsWith("Date('"))
        assertTrue(result.endsWith("')"))
    }

    @Test
    fun `10-digit number is converted to Timestamp marker`() {
        val result = 1705312200L.convertDateTypes()
        assertTrue(result is String)
        assertEquals("Timestamp('1705312200')", result)
    }

    @Test
    fun `small numbers are not converted`() {
        assertEquals(42, 42.convertDateTypes())
        assertEquals(999999999, 999999999.convertDateTypes())
    }

    @Test
    fun `strings are not converted`() {
        assertEquals("hello", "hello".convertDateTypes())
    }

    @Test
    fun `null is not converted`() {
        assertNull(null.convertDateTypes())
    }

    @Test
    fun `map values are recursively converted`() {
        val date = Date(1705312200000L)
        val map = mapOf("name" to "Alice", "createdAt" to date)
        val result = map.convertDateTypes()
        assertTrue(result is Map<*, *>)
        val resultMap = result as Map<*, *>
        assertEquals("Alice", resultMap["name"])
        assertTrue((resultMap["createdAt"] as String).startsWith("Date('"))
    }

    @Test
    fun `nested maps are recursively converted`() {
        val date = Date(1705312200000L)
        val map = mapOf("data" to mapOf("timestamp" to date))
        val result = map.convertDateTypes()
        val resultMap = result as Map<*, *>
        val innerMap = resultMap["data"] as Map<*, *>
        assertTrue((innerMap["timestamp"] as String).startsWith("Date('"))
    }

    @Test
    fun `list elements are recursively converted`() {
        val date = Date(1705312200000L)
        val list = listOf(date, 1705312200L)
        val result = list.convertDateTypes()
        assertTrue(result is List<*>)
        val resultList = result as List<*>
        assertTrue((resultList[0] as String).startsWith("Date('"))
        assertEquals("Timestamp('1705312200')", resultList[1])
    }

    @Test
    fun `11-digit number is converted to Timestamp`() {
        val result = 17053122000L.convertDateTypes()
        assertEquals("Timestamp('17053122000')", result)
    }

    @Test
    fun `negative number is not converted`() {
        assertEquals(-1, (-1).convertDateTypes())
        assertEquals(-10000000000L, (-10000000000L).convertDateTypes())
    }
}
