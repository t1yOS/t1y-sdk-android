package com.t1y.sdk.network

import com.t1y.sdk.crypto.AesGcm
import com.t1y.sdk.exception.T1YException
import kotlinx.serialization.json.*
import okhttp3.Response

/** Shared JSON instance for response handling. */
private val responseJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/**
 * Processes the raw OkHttp response:
 * 1. Reads the response body
 * 2. Decrypts if safe mode and response contains a `j` field
 * 3. Formats timestamps (`createdAt`/`updatedAt`) to local time
 * 4. Throws [T1YException] for non-2xx status codes
 *
 * @param response The OkHttp response.
 * @param isSafeMode Whether AES-256-GCM decryption should be attempted.
 * @param secretKey 32-character secret key for decryption.
 * @param timeFormat Timestamp format string (e.g. "YYYY-MM-DD HH:mm:ss").
 * @return The processed JSON response body string.
 */
internal fun handleResponse(
    response: Response,
    isSafeMode: Boolean,
    secretKey: String,
    timeFormat: String
): String {
    val bodyString = response.body?.string() ?: ""

    // Process body: decrypt if needed, otherwise pass through
    val processedBody = if (isSafeMode && bodyString.isNotBlank()) {
        try {
            val jsonElement = responseJson.parseToJsonElement(bodyString)
            if (jsonElement is JsonObject && jsonElement.containsKey("j")) {
                // Encrypted payload detected — decrypt it
                val keyBytes = secretKey.toByteArray(Charsets.UTF_8)
                AesGcm.decrypt(bodyString, keyBytes)
            } else {
                bodyString
            }
        } catch (e: Exception) {
            // If decryption fails, return the original body
            // (the error will be surfaced by the non-2xx check below)
            bodyString
        }
    } else {
        bodyString
    }

    // For non-2xx responses, throw T1YException with server error details
    if (!response.isSuccessful) {
        throw T1YException(
            code = response.code,
            message = response.message.ifBlank { "HTTP error ${response.code}" },
            data = processedBody
        )
    }

    // Format timestamps in the response
    return try {
        formatTimestampsInJson(processedBody, timeFormat)
    } catch (e: Exception) {
        // If timestamp formatting fails, return the raw body
        processedBody
    }
}

/**
 * Recursively formats `createdAt` and `updatedAt` timestamp fields in JSON
 * from UTC strings to local time using the given format.
 */
private fun formatTimestampsInJson(jsonString: String, timeFormat: String): String {
    val element = responseJson.parseToJsonElement(jsonString)
    val formatted = formatTimestampsRecursive(element, timeFormat)
    return responseJson.encodeToString(JsonElement.serializer(), formatted)
}

private fun formatTimestampsRecursive(element: JsonElement, format: String): JsonElement {
    return when (element) {
        is JsonObject -> {
            val mutable = element.toMutableMap()
            for (key in mutable.keys) {
                // Format known timestamp fields
                if ((key == "createdAt" || key == "updatedAt") && mutable[key] is JsonPrimitive) {
                    val prim = mutable[key] as JsonPrimitive
                    if (prim.isString) {
                        mutable[key] = JsonPrimitive(formatTimestamp(prim.content, format))
                    }
                } else {
                    mutable[key] = formatTimestampsRecursive(mutable[key]!!, format)
                }
            }
            JsonObject(mutable)
        }
        is JsonArray -> JsonArray(element.map { formatTimestampsRecursive(it, format) })
        else -> element
    }
}

/**
 * Formats a UTC timestamp string to local time using the given format.
 *
 * Format tokens:
 * - YYYY → 4-digit year
 * - MM → 2-digit month
 * - DD → 2-digit day
 * - HH → 2-digit hour (24h)
 * - mm → 2-digit minute
 * - ss → 2-digit second
 */
private fun formatTimestamp(utcStr: String, format: String): String {
    return try {
        // Parse ISO 8601 or common date formats
        val instant = try {
            java.time.Instant.parse(utcStr)
        } catch (e: Exception) {
            // Try parsing as timestamp in milliseconds
            java.time.Instant.ofEpochMilli(utcStr.toLong())
        }
        val localDateTime = java.time.LocalDateTime.ofInstant(
            instant,
            java.time.ZoneId.systemDefault()
        )
        format
            .replace("YYYY", localDateTime.year.toString().padStart(4, '0'))
            .replace("MM", localDateTime.monthValue.toString().padStart(2, '0'))
            .replace("DD", localDateTime.dayOfMonth.toString().padStart(2, '0'))
            .replace("HH", localDateTime.hour.toString().padStart(2, '0'))
            .replace("mm", localDateTime.minute.toString().padStart(2, '0'))
            .replace("ss", localDateTime.second.toString().padStart(2, '0'))
    } catch (e: Exception) {
        utcStr // Return original if parsing fails
    }
}
