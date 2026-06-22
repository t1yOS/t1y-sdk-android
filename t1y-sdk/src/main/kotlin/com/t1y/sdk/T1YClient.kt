package com.t1y.sdk

import com.t1y.sdk.crypto.AesGcm
import com.t1y.sdk.crypto.Hmac
import com.t1y.sdk.crypto.Signer
import com.t1y.sdk.exception.T1YException
import com.t1y.sdk.model.ApiResponse
import com.t1y.sdk.model.CollectionsResult
import com.t1y.sdk.model.InitResult
import com.t1y.sdk.model.toJsonElement
import com.t1y.sdk.network.createOkHttpClient
import com.t1y.sdk.network.executeSuspend
import com.t1y.sdk.network.handleResponse
import com.t1y.sdk.util.TypeChecks
import com.t1y.sdk.util.Validation
import com.t1y.sdk.util.convertDateTypes
import com.t1y.sdk.util.encodeUrl
import com.t1y.sdk.util.extractPathAndQuery
import com.t1y.sdk.util.toQueryString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.Closeable

// Re-exports for convenience
typealias T1YOS = T1YClient

/**
 * t1yOS Serverless Platform Kotlin SDK client.
 *
 * ## Usage
 * ```
 * val client = T1YClient(Config(
 *     appId = 1001,
 *     apiKey = "your-32-char-api-key-here!!",
 *     secretKey = "your-32-char-secret-key-here!"
 * ))
 *
 * // Initialize (sync time offset and safe mode)
 * client.init()
 *
 * // Use the database
 * client.db.collection("users").insertOne(mapOf("name" to "Alice"))
 *
 * // Call a cloud function
 * client.callFunc("hello")
 * ```
 */
class T1YClient(config: Config) : Closeable {

    /** Internal mutable configuration. Updated by [init] with server-synced values. */
    internal val mutableConfig = MutableConfig(config)

    /** Shared OkHttp client instance. */
    private val httpClient = createOkHttpClient()

    /** Whether this client has been closed. */
    private var closed = false

    /** Shared JSON instance with lenient parsing. */
    @PublishedApi internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /** Database accessor for chainable collection operations. */
    val db: DatabaseAccessor = DatabaseAccessor()

    // ===== INITIALIZATION =====

    /**
     * Syncs the time offset and safe mode setting with the t1yOS server.
     *
     * Calls `GET /init/:appId`. On success, updates the internal [offset]
     * and [isSafeMode] configuration. On failure, gracefully degrades to
     * default values (offset=0, safeMode=false) matching JS SDK behavior.
     */
    suspend fun init() {
        try {
            val response = requestRaw("GET", "/init/${mutableConfig.appId}")
            val data = response.data
            if (data is JsonObject) {
                // Parse InitResult once and reuse — but only apply fields
                // that were actually present in the response
                val initResult = try {
                    json.decodeFromJsonElement<InitResult>(data)
                } catch (e: Exception) {
                    null
                }
                if (initResult != null) {
                    if (data.containsKey("is_safe_mode")) {
                        mutableConfig.isSafeMode = initResult.isSafeMode
                    }
                    if (data.containsKey("unix")) {
                        mutableConfig.offset =
                            (initResult.unix - System.currentTimeMillis() / 1000).toInt()
                    }
                }
            }
        } catch (_: Exception) {
            // Gracefully degrade on failure (matching JS SDK behavior)
            mutableConfig.isSafeMode = false
            mutableConfig.offset = 0
        }
    }

    // ===== PUBLIC API =====

    /**
     * Retrieves application metadata.
     *
     * @param field Optional specific metadata field to retrieve.
     * @return API response containing the metadata.
     */
    suspend fun getMeta(field: String? = null): ApiResponse<JsonElement> {
        val path = if (!field.isNullOrBlank()) {
            "/${Constants.API_VERSION}/meta?field=${field.encodeUrl()}"
        } else {
            "/${Constants.API_VERSION}/meta"
        }
        return requestRaw("GET", path)
    }

    /**
     * Checks whether a newer application version is available on the server.
     *
     * @return true if the server version is greater than the client version.
     */
    suspend fun checkUpdate(): Boolean {
        return try {
            val response = getMeta("version")
            val data = response.data
            if (data is JsonObject) {
                val serverVersion = data["result"]?.let {
                    try {
                        json.decodeFromJsonElement<Int>(it)
                    } catch (e: Exception) {
                        null
                    }
                } ?: return false
                serverVersion > mutableConfig.version
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Calls a cloud function (`.jsc` file) on the t1yOS server.
     *
     * The function name is automatically normalized:
     * - `hello` → `hello.jsc`
     * - `hello.js` → `hello.jsc`
     * - `dir/` → `dir/index.jsc`
     *
     * @param name Function name (with or without `.jsc` extension).
     * @param params Optional parameters to pass to the function.
     * @param enableSafeMode Optional override for safe mode encryption.
     * @return API response with the function's return value.
     */
    suspend fun callFunc(
        name: String,
        params: Any? = null,
        enableSafeMode: Boolean? = null
    ): ApiResponse<JsonElement> {
        val normalizedName = name.ensureJscExtension()
        val path = "/${mutableConfig.appId}/$normalizedName"
        return requestRaw("POST", path, params, enableSafeMode)
    }

    // ===== CORE REQUEST METHODS =====

    /**
     * Sends a typed request to the t1yOS API.
     *
     * Use this for well-known response types. The type [T] must be annotated
     * with [Serializable]. Uses reified generics for compile-time safety.
     *
     * @param method HTTP method (GET, POST, PUT, DELETE).
     * @param path URL path (e.g., "/v5/classes/users").
     * @param params Request body or query parameters.
     * @param encryption Optional safe-mode override.
     * @return Typed [ApiResponse] with deserialized data.
     */
    suspend inline fun <reified T : @Serializable Any> request(
        method: String,
        path: String,
        params: Any? = null,
        encryption: Boolean? = null
    ): ApiResponse<T> {
        val responseJson = executeRequest(method, path, params, encryption)
        return try {
            json.decodeFromString(responseJson)
        } catch (e: Exception) {
            throw T1YException(
                code = 0,
                message = "Failed to decode response: ${e.message}",
                data = responseJson
            )
        }
    }

    /**
     * Sends a raw request to the t1yOS API, returning [JsonElement] data.
     *
     * Use this for dynamic queries where the response shape is not known
     * at compile time (e.g., find results with user-defined schemas).
     *
     * @param method HTTP method (GET, POST, PUT, DELETE).
     * @param path URL path (e.g., "/v5/classes/users/find").
     * @param params Request body or query parameters.
     * @param encryption Optional safe-mode override.
     * @return [ApiResponse] with raw [JsonElement] data.
     */
    suspend fun requestRaw(
        method: String,
        path: String,
        params: Any? = null,
        encryption: Boolean? = null
    ): ApiResponse<JsonElement> {
        val responseJson = executeRequest(method, path, params, encryption)
        return try {
            json.decodeFromString(responseJson)
        } catch (e: Exception) {
            throw T1YException(
                code = 0,
                message = "Failed to decode response: ${e.message}",
                data = responseJson
            )
        }
    }

    // ===== INTERNAL REQUEST EXECUTION =====

    /**
     * Executes the full request lifecycle:
     *
     * 1. Converts Date objects and large numbers to server marker strings
     * 2. Builds URL, serializes body, applies safe-mode encryption
     * 3. Computes HMAC-SHA256 signature
     * 4. Injects authentication headers
     * 5. Sends via OkHttp
     * 6. Handles response (decryption, timestamp formatting, error wrapping)
     *
     * @return The processed JSON response body string.
     */
    @PublishedApi internal suspend fun executeRequest(
        method: String,
        path: String,
        params: Any?,
        encryption: Boolean?
    ): String = withContext(Dispatchers.IO) {
        val safeMode = encryption ?: mutableConfig.isSafeMode
        val baseUrl = mutableConfig.baseUrl.trimEnd('/')

        // Build request body
        var bodyString: String? = null
        var rawBodyForSigning = ""

        if (method != "GET" && params != null) {
            val convertedParams = params.convertDateTypes()
            val jsonBody = json.encodeToString(
                kotlinx.serialization.serializer(),
                convertedParams.toJsonElement()
            )
            bodyString = if (safeMode) {
                val encrypted = AesGcm.encrypt(jsonBody, mutableConfig.secretKeyBytes)
                rawBodyForSigning = encrypted
                encrypted
            } else {
                rawBodyForSigning = jsonBody
                jsonBody
            }
        }

        // Build URL: baseUrl + path + queryString (query string after path)
        val urlBuilder = StringBuilder(baseUrl)
        urlBuilder.append(path)
        if (method == "GET" && params is Map<*, *> && params.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            urlBuilder.append(params.toQueryString())
        }

        // Compute timestamp and signature
        val timestamp = Signer.getSafeTimestampLong(mutableConfig.offset)
        val fullUrl = urlBuilder.toString()
        val pathAndQuery = fullUrl.extractPathAndQuery()

        val signature = Signer.createSignature(
            method = method,
            pathAndQuery = pathAndQuery,
            body = rawBodyForSigning,
            appId = mutableConfig.appId,
            timestamp = timestamp,
            secretKeyBytes = mutableConfig.secretKeyBytes
        )

        // Build OkHttp request
        val requestBuilder = Request.Builder()
            .url(fullUrl)
            .header("X-T1Y-Application-ID", mutableConfig.appId.toString())
            .header("X-T1Y-API-Key", mutableConfig.apiKey)
            .header("X-T1Y-Safe-Timestamp", timestamp.toString())
            .header("X-T1Y-Safe-Sign", signature)

        if (method != "GET" && bodyString != null) {
            requestBuilder.header("Content-Type", "application/json")
            requestBuilder.method(
                method,
                bodyString.toRequestBody("application/json".toMediaType())
            )
        } else if (method == "GET" || method == "DELETE" || method == "HEAD") {
            // Methods that do not require a body
            requestBuilder.method(method, null)
        } else {
            // POST, PUT, PATCH with no params — send empty JSON body
            requestBuilder.header("Content-Type", "application/json")
            requestBuilder.method(
                method,
                "".toRequestBody("application/json".toMediaType())
            )
        }

        // Execute and handle response
        val response = httpClient.executeSuspend(requestBuilder.build())
        handleResponse(response, safeMode, mutableConfig.secretKeyBytes, mutableConfig.timeFormat)
    }

    // ===== JSC EXTENSION NORMALIZATION =====

    /**
     * Normalizes a cloud function name to include the `.jsc` extension.
     *
     * Rules:
     * - `hello` → `hello.jsc`
     * - `hello.jsc` → `hello.jsc` (unchanged)
     * - `hello.js` → `hello.jsc`
     * - `dir/` → `dir/index.jsc`
     * - Query strings and fragments are preserved
     */
    private fun String.ensureJscExtension(): String {
        // Separate base path from query/fragment
        val hashIndex = indexOf('#')
        val queryIndex = indexOf('?')
        val suffixStart = when {
            hashIndex >= 0 && queryIndex >= 0 -> minOf(hashIndex, queryIndex)
            hashIndex >= 0 -> hashIndex
            queryIndex >= 0 -> queryIndex
            else -> -1
        }
        val basePath = if (suffixStart >= 0) substring(0, suffixStart) else this
        val suffix = if (suffixStart >= 0) substring(suffixStart) else ""

        val normalized = when {
            basePath.endsWith('/') -> "${basePath}index.jsc"
            basePath.endsWith(".jsc", ignoreCase = true) -> basePath
            basePath.endsWith(".js", ignoreCase = true) ->
                basePath.substring(0, basePath.length - 3) + ".jsc"
            else -> "$basePath.jsc"
        }

        return "$normalized$suffix"
    }

    // ===== UTILITY METHODS =====

    /**
     * Validates that the given string is a valid 24-character hex ObjectID.
     *
     * @param idStr The string to validate.
     * @param name Optional name for error messages.
     * @return true if valid.
     */
    fun assertObjectID(idStr: String, name: String = "ObjectID"): Boolean =
        Validation.assertObjectID(idStr, name)

    /**
     * Returns true if [value] is a non-null, non-empty Map.
     */
    fun isNonEmptyObject(value: Any?): Boolean =
        TypeChecks.isNonEmptyObject(value)

    /**
     * Returns true if [value] is a non-null Map.
     */
    fun isPlainObject(value: Any?): Boolean =
        TypeChecks.isPlainObject(value)

    /**
     * Returns true if [value] is a non-empty List of non-empty Maps.
     */
    fun isNonEmptyArrayWithNonEmptyObjects(value: Any?): Boolean =
        TypeChecks.isNonEmptyArrayWithNonEmptyObjects(value)

    /**
     * Computes the HMAC-SHA256 of [message] using [secret].
     *
     * @return 64-character lowercase hex digest.
     */
    fun hmacSHA256(secret: String, message: String): String =
        Hmac.hmacSHA256Hex(secret, message)

    /**
     * Verifies the HMAC-SHA256 [signature] of [message] using [secret].
     *
     * Uses a timing-safe comparison to prevent side-channel attacks.
     *
     * @return true if the signature is valid.
     */
    fun verifyHmacSHA256(secret: String, message: String, signature: String): Boolean =
        Hmac.verifyHmacSHA256(secret, message, signature)

    /**
     * Cleans up resources held by this client.
     *
     * After calling this method, the client should not be used.
     * This method:
     * - Shuts down the OkHttp dispatcher and evicts idle connections
     * - Zeros out the secret key bytes in memory
     */
    override fun close() {
        if (closed) return
        closed = true
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        mutableConfig.clearSecretKey()
    }

    // ===== DATABASE ACCESSOR =====

    /**
     * Database accessor providing chainable collection operations.
     *
     * Usage:
     * ```
     * client.db.collection("users").insertOne(...)
     * client.db.toObjectID("507f1f77bcf86cd799439011")
     * client.db.getCollections()
     * ```
     */
    inner class DatabaseAccessor internal constructor() {

        /**
         * Returns a [T1Collection] instance for the given collection name.
         *
         * @param name Collection name.
         * @return [T1Collection] ready for CRUD operations.
         */
        fun collection(name: String): T1Collection =
            T1Collection(this@T1YClient, name)

        /**
         * Creates an ObjectID marker string from a valid 24-character hex string.
         *
         * @param id 24-character hex ObjectID.
         * @return Marker string like `ObjectID('507f1f77bcf86cd799439011')`.
         */
        fun toObjectID(id: String): String {
            assertObjectID(id)
            return "ObjectID('$id')"
        }

        /**
         * Lists all collections (schemas) in the application.
         *
         * @return API response with list of collection names.
         */
        suspend fun getCollections(): ApiResponse<CollectionsResult> =
            request<CollectionsResult>("GET", "/${Constants.API_VERSION}/schemas")
    }
}
