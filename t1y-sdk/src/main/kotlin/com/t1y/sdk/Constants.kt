package com.t1y.sdk

/**
 * t1yOS Serverless Platform SDK constants.
 */
object Constants {
    /** Default server base URL. */
    const val DEFAULT_BASE_URL = "https://myapp.t1y.net"

    /** Minimum valid application ID. */
    const val MIN_APP_ID = 1001

    /** API Key must be exactly 32 characters. */
    const val API_KEY_LENGTH = 32

    /** Secret Key must be exactly 32 characters. */
    const val SECRET_KEY_LENGTH = 32

    /** Default application version. */
    const val DEFAULT_VERSION = 0

    /** Default timestamp format (YYYY-MM-DD HH:mm:ss). */
    const val DEFAULT_TIME_FORMAT = "YYYY-MM-DD HH:mm:ss"

    /** Default time offset in seconds. */
    const val DEFAULT_OFFSET = 0

    /** Default safe mode (AES-256-GCM disabled by default). */
    const val DEFAULT_SAFE_MODE = false

    /** Maximum allowed time difference in seconds (10 seconds). */
    const val MAX_TIME_DIFF = 10

    /** Request timeout in milliseconds (5 minutes). */
    const val REQUEST_TIMEOUT_MS = 300_000L

    /** Maximum results per page for find queries. */
    const val MAX_PAGE_SIZE = 100

    /** Default page size for find queries. */
    const val DEFAULT_PAGE_SIZE = 10

    /** MongoDB ObjectID must be exactly 24 hex characters. */
    const val OBJECT_ID_LENGTH = 24

    /** API version prefix. */
    const val API_VERSION = "v5"

    /** Regex pattern for valid MongoDB ObjectID hex strings. */
    val OBJECT_ID_REGEX = Regex("^[0-9a-fA-F]{24}$")
}
