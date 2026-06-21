package com.t1y.sdk.special

import com.t1y.sdk.Constants
import kotlinx.serialization.json.Json

// ===== ObjectID =====

/**
 * Creates an ObjectID marker string.
 *
 * The Go server recognizes this and converts it to a MongoDB ObjectID.
 *
 * @param id 24-character hex string.
 * @return Marker string like `ObjectID('507f1f77bcf86cd799439011')`.
 */
fun ObjectID(id: String): String {
    require(Constants.OBJECT_ID_REGEX.matches(id)) {
        "Invalid ObjectID: \"$id\" — must be 24 hex characters"
    }
    return "ObjectID('$id')"
}

// ===== Date Types =====

/** Creates a Date marker. Server converts to `time.Time`. */
fun Date(value: String): String = "Date('$value')"

/** Creates a DateTime marker. Server converts to `time.Time`. */
fun DateTime(value: String): String = "DateTime('$value')"

/** Creates a Timestamp marker. Server converts to Unix timestamp. */
fun Timestamp(value: Long): String = "Timestamp('$value')"

/** Creates a Timestamp marker from a string. Server converts to Unix timestamp. */
fun Timestamp(value: String): String = "Timestamp('$value')"

// ===== Numeric Types =====

/** Creates a Boolean marker. Server converts to `bool`. */
fun Boolean(value: kotlin.Boolean): String = "Boolean($value)"

/** Creates an Integer marker. Server converts to `int32`. */
fun Integer(n: Int): String = "Integer($n)"

/** Creates a Bigint marker. Server converts to `int64`. */
fun Bigint(n: Long): String = "Bigint($n)"

/** Creates a Float marker. Server converts to `float32`. */
fun Float(n: Double): String = "Float($n)"

/** Creates a Double marker. Server converts to `float64`. */
fun Double(n: Double): String = "Double($n)"

// ===== Structured Types =====

/** Creates an Array marker. Server converts to Go slice. */
fun Array(arr: List<Any?>): String =
    "Array(${Json.encodeToString(kotlinx.serialization.serializer<List<kotlinx.serialization.json.JsonElement>>(), arr.map { it.toJsonPrimitiveOrString() })})"

/** Creates a Map marker. Server converts to `map[string]interface{}`. */
fun Map(obj: Map<String, Any?>): String =
    "Map(${Json.encodeToString(kotlinx.serialization.serializer<kotlinx.serialization.json.JsonObject>(), obj.toJsonObject())})"

/** Creates a MapArray marker. Server converts to `[]map[string]interface{}`. */
fun MapArray(arr: List<Map<String, Any?>>): String =
    "Map[](${Json.encodeToString(kotlinx.serialization.serializer<kotlinx.serialization.json.JsonArray>(), arr.toJsonArray())})"

// ===== Null Markers =====

/** Represents a Go `nil` value. */
const val Null = "Null"

/** Represents a Go `nil` value. */
const val None = "None"

/** Represents a Go `nil` value. */
const val Nil = "Nil"

/** Represents an empty string (Go `nil`). */
const val Empty = ""

/** Represents BSON Undefined. */
const val UNDEFINED = "UNDEFINED"

/** Represents BSON Undefined. */
const val Undefined = "Undefined"

// ===== Time Helpers (server-side time) =====

/** Server's current UTC time. */
const val TIME_NOW = "time.Now()"

/** Server's current Unix timestamp in seconds. */
const val TIME_NOW_UNIX = "time.Now().Unix()"

/** Server's current UnixNano timestamp. */
const val TIME_NOW_UNIX_NANO = "time.Now().UnixNano()"

/** Server's current weekday (English). */
const val TIME_NOW_WEEKDAY = "time.Now().Weekday()"

/** Server's current weekday (Chinese). */
const val TIME_NOW_WEEKDAY_CHINESE = "time.Now().Weekday().Chinese()"

/**
 * Convenience object grouping all server-time helpers.
 * Usage: `timeNow.Now()`, `timeNow.NowUnix()`, etc.
 */
object timeNow {
    @JvmStatic fun Now(): String = TIME_NOW
    @JvmStatic fun NowUnix(): String = TIME_NOW_UNIX
    @JvmStatic fun NowUnixNano(): String = TIME_NOW_UNIX_NANO
    @JvmStatic fun NowWeekday(): String = TIME_NOW_WEEKDAY
    @JvmStatic fun NowWeekdayChinese(): String = TIME_NOW_WEEKDAY_CHINESE
}

// ===== Internal Helpers =====

private fun Any?.toJsonPrimitiveOrString(): kotlinx.serialization.json.JsonElement {
    return when (this) {
        null -> kotlinx.serialization.json.JsonNull
        is kotlinx.serialization.json.JsonElement -> this
        is String -> kotlinx.serialization.json.JsonPrimitive(this)
        is kotlin.Boolean -> kotlinx.serialization.json.JsonPrimitive(this)
        is Int -> kotlinx.serialization.json.JsonPrimitive(this)
        is Long -> kotlinx.serialization.json.JsonPrimitive(this)
        is Double -> kotlinx.serialization.json.JsonPrimitive(this)
        is Float -> kotlinx.serialization.json.JsonPrimitive(this.toDouble())
        is Number -> kotlinx.serialization.json.JsonPrimitive(toString())
        else -> kotlinx.serialization.json.JsonPrimitive(toString())
    }
}

private fun Map<String, Any?>.toJsonObject(): kotlinx.serialization.json.JsonObject {
    return kotlinx.serialization.json.buildJsonObject {
        this@toJsonObject.forEach { (key, value) ->
            put(key, value.toJsonPrimitiveOrString())
        }
    }
}

private fun List<Map<String, Any?>>.toJsonArray(): kotlinx.serialization.json.JsonArray {
    return kotlinx.serialization.json.buildJsonArray {
        this@toJsonArray.forEach { map ->
            add(map.toJsonObject())
        }
    }
}
