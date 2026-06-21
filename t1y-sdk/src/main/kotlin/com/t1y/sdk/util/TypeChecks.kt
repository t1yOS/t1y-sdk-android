package com.t1y.sdk.util

/**
 * Type-checking utility functions for runtime object inspection.
 * These mirror the JS SDK's utility checks.
 */
object TypeChecks {

    /**
     * Returns true if [value] is a non-null, non-empty Map.
     */
    fun isNonEmptyObject(value: Any?): Boolean =
        value is Map<*, *> && value.isNotEmpty()

    /**
     * Returns true if [value] is a non-null Map (including empty Maps, excluding null, arrays, etc.).
     */
    fun isPlainObject(value: Any?): Boolean =
        value is Map<*, *>

    /**
     * Returns true if [value] is a non-empty List where every element
     * is a non-empty Map.
     */
    fun isNonEmptyArrayWithNonEmptyObjects(value: Any?): Boolean =
        value is List<*> && value.isNotEmpty() &&
                value.all { it is Map<*, *> && it.isNotEmpty() }
}
