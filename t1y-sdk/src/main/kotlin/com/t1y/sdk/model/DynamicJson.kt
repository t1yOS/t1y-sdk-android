package com.t1y.sdk.model

import kotlinx.serialization.json.*

/**
 * Converts a [JsonElement] to its Kotlin native representation.
 *
 * - [JsonNull] → null
 * - [JsonPrimitive] → String, Boolean, or Number
 * - [JsonObject] → Map<String, Any?>
 * - [JsonArray] → List<Any?>
 */
fun JsonElement.toAny(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> {
        if (isString) content
        else if (content == "true" || content == "false") boolean
        else longOrNull ?: doubleOrNull ?: intOrNull ?: content
    }
    is JsonObject -> toMap()
    is JsonArray -> toList()
}

/**
 * Converts a [JsonObject] to a [Map] of [String] to [Any]?.
 */
fun JsonObject.toMap(): Map<String, Any?> =
    mapValues { (_, value) -> value.toAny() }

/**
 * Converts a [JsonArray] to a [List] of [Any]?.
 */
fun JsonArray.toList(): List<Any?> =
    map { it.toAny() }

/**
 * Extension to convert any value to a [JsonElement].
 * Handles common Kotlin types: Map, List, String, Number, Boolean, null.
 */
fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is Map<*, *> -> buildJsonObject {
        @Suppress("UNCHECKED_CAST")
        (this@toJsonElement as Map<String, Any?>).forEach { (key, value) ->
            put(key, value.toJsonElement())
        }
    }
    is List<*> -> buildJsonArray {
        this@toJsonElement.forEach { add(it.toJsonElement()) }
    }
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Int -> JsonPrimitive(this)
    is Long -> JsonPrimitive(this)
    is Double -> JsonPrimitive(this)
    is Float -> JsonPrimitive(this.toDouble())
    is Number -> JsonPrimitive(this)
    else -> JsonPrimitive(toString())
}
