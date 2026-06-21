package com.t1y.sdk.util

import java.net.URLEncoder

/**
 * Recursively converts `java.util.Date` objects to `Date('ISO-8601')` marker strings
 * and 10+ digit numbers to `Timestamp('...')` marker strings.
 *
 * This matches the JS SDK's `convertDateTypes()` behavior.
 *
 * @return The converted value with Date and long-number types replaced by markers.
 */
fun Any?.convertDateTypes(): Any? = when (this) {
    is java.util.Date -> "Date('${this.toInstant().toString()}')"
    is Number -> {
        val str = toString()
        // 10+ digit number → Timestamp marker
        if (str.length >= 10 && str.matches(Regex("^\\d{10,}$"))) {
            "Timestamp('$str')"
        } else {
            this
        }
    }
    is Map<*, *> -> entries.associate { (k, v) -> k to v.convertDateTypes() }
    is List<*> -> map { it.convertDateTypes() }
    else -> this
}

/**
 * Builds a URL query string from a Map.
 *
 * Keys with null values are skipped.
 * String values are URL-encoded.
 * Other values are converted to string and URL-encoded.
 *
 * @return Query string starting with "?" if there are parameters, empty string otherwise.
 */
fun Map<*, *>.toQueryString(): String {
    val params = mapNotNull { (key, value) ->
        if (value == null) return@mapNotNull null
        val encodedKey = URLEncoder.encode(key.toString(), "UTF-8")
        val encodedValue = URLEncoder.encode(value.toString(), "UTF-8")
        "$encodedKey=$encodedValue"
    }
    return if (params.isEmpty()) "" else "?" + params.joinToString("&")
}

/**
 * URL-encodes a string for use in query parameters.
 */
fun String.encodeUrl(): String = URLEncoder.encode(this, "UTF-8")

/**
 * Extracts the path and query string from a full URL.
 *
 * Example: "https://myapp.t1y.net/v5/classes/users?page=1" → "/v5/classes/users?page=1"
 */
fun String.extractPathAndQuery(): String {
    val withoutScheme = substringAfter("://")  // myapp.t1y.net/v5/classes/users?page=1
    val hostEnd = withoutScheme.indexOf('/')
    return if (hostEnd >= 0) withoutScheme.substring(hostEnd) else "/"
}
