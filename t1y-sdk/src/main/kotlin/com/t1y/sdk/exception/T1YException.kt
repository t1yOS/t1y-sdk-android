package com.t1y.sdk.exception

/**
 * API error returned by the t1yOS server or thrown for network errors.
 *
 * @property code HTTP status code or error code (0 for network errors, 408 for timeout).
 * @property message Human-readable error message.
 * @property data Optional additional error data from the server.
 */
class T1YException(
    val code: Int,
    override val message: String,
    val data: Any? = null
) : RuntimeException(message) {

    override fun toString(): String =
        "T1YException(code=$code, message='$message', data=$data)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is T1YException) return false
        return code == other.code && message == other.message
    }

    override fun hashCode(): Int {
        var result = code
        result = 31 * result + message.hashCode()
        return result
    }
}

/**
 * Validation error for client-side parameter validation failures.
 */
class ValidationException(
    override val message: String
) : IllegalArgumentException(message) {

    override fun toString(): String =
        "ValidationException(message='$message')"
}
