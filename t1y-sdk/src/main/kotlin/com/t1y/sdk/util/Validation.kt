package com.t1y.sdk.util

import com.t1y.sdk.Config
import com.t1y.sdk.Constants
import com.t1y.sdk.exception.ValidationException

/**
 * Client-side input validators.
 * All validation functions throw [ValidationException] on failure
 * rather than returning boolean, to catch configuration errors early.
 */
object Validation {

    /**
     * Validates the entire config, throwing on any invalid field.
     */
    fun validateConfig(config: Config) {
        validateAppId(config.appId)
        validateApiKey(config.apiKey)
        validateSecretKey(config.secretKey)
        validateBaseUrl(config.baseUrl)
        require(config.version >= 0) { "version must be non-negative, got: ${config.version}" }
    }

    /**
     * Validates that [appId] is an integer >= [Constants.MIN_APP_ID].
     */
    fun validateAppId(appId: Int) {
        require(appId >= Constants.MIN_APP_ID) {
            "appId must be >= ${Constants.MIN_APP_ID}, got: $appId"
        }
    }

    /**
     * Validates that [apiKey] is exactly [Constants.API_KEY_LENGTH] characters.
     */
    fun validateApiKey(apiKey: String) {
        require(apiKey.length == Constants.API_KEY_LENGTH) {
            "apiKey must be exactly ${Constants.API_KEY_LENGTH} characters, got: ${apiKey.length}"
        }
    }

    /**
     * Validates that [secretKey] is exactly [Constants.SECRET_KEY_LENGTH] characters.
     */
    fun validateSecretKey(secretKey: String) {
        require(secretKey.length == Constants.SECRET_KEY_LENGTH) {
            "secretKey must be exactly ${Constants.SECRET_KEY_LENGTH} characters, got: ${secretKey.length}"
        }
    }

    /**
     * Validates that [baseUrl] starts with http:// or https://.
     */
    fun validateBaseUrl(baseUrl: String) {
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "baseUrl must start with http:// or https://, got: $baseUrl"
        }
    }

    /**
     * Validates that [idStr] is a valid 24-character hex ObjectID string.
     *
     * @param idStr The string to validate.
     * @param name Optional name for the error message (default: "ObjectID").
     * @return true if valid.
     * @throws ValidationException if invalid.
     */
    fun assertObjectID(idStr: String, name: String = "ObjectID"): Boolean {
        require(idStr.length == Constants.OBJECT_ID_LENGTH) {
            "$name length must be exactly ${Constants.OBJECT_ID_LENGTH} characters, got: ${idStr.length}"
        }
        require(Constants.OBJECT_ID_REGEX.matches(idStr)) {
            "$name must be a 24-character hex string, got: \"$idStr\""
        }
        return true
    }

    /**
     * Validates that [value] is a non-empty, non-array object (Map).
     */
    fun requireNonEmptyObject(value: Any?, name: String = "parameter") {
        require(requireNonEmptyObjectOrNull(value, name)) {
            "$name must be a non-empty plain object"
        }
    }

    private fun requireNonEmptyObjectOrNull(value: Any?, name: String = "parameter"): Boolean {
        if (value == null) {
            throw ValidationException("$name must be a non-empty plain object, got: null")
        }
        if (value !is Map<*, *>) {
            throw ValidationException("$name must be a non-empty plain object, got: ${value::class.simpleName}")
        }
        if (value.isEmpty()) {
            throw ValidationException("$name must be a non-empty plain object, got: empty map")
        }
        return true
    }

    /**
     * Validates that [value] is a non-null plain object (Map).
     */
    fun requirePlainObject(value: Any?, name: String = "parameter") {
        if (value == null) {
            throw ValidationException("$name must be a plain object, got: null")
        }
        if (value !is Map<*, *>) {
            throw ValidationException("$name must be a plain object, got: ${value::class.simpleName}")
        }
    }

    /**
     * Validates that [value] is a non-empty list of non-empty objects.
     */
    fun requireNonEmptyArrayWithNonEmptyObjects(value: Any?, name: String = "parameter") {
        if (value == null) {
            throw ValidationException("$name must be a non-empty array of non-empty objects, got: null")
        }
        if (value !is List<*>) {
            throw ValidationException(
                "$name must be a non-empty array of non-empty objects, got: ${value::class.simpleName}"
            )
        }
        if (value.isEmpty()) {
            throw ValidationException("$name must be a non-empty array of non-empty objects, got: empty list")
        }
        value.forEachIndexed { index, item ->
            if (item !is Map<*, *> || item.isEmpty()) {
                throw ValidationException(
                    "$name[$index] must be a non-empty plain object, got: ${item?.let { it::class.simpleName } ?: "null"}"
                )
            }
        }
    }
}
