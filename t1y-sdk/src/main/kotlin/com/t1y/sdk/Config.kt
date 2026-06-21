package com.t1y.sdk

import com.t1y.sdk.util.Validation

/**
 * Configuration for the t1yOS client.
 *
 * @property baseUrl Server base URL, defaults to [Constants.DEFAULT_BASE_URL].
 * @property appId Application ID, must be >= [Constants.MIN_APP_ID].
 * @property apiKey API Key, must be exactly [Constants.API_KEY_LENGTH] characters.
 * @property secretKey Secret Key, must be exactly [Constants.SECRET_KEY_LENGTH] characters.
 * @property version Application version, defaults to [Constants.DEFAULT_VERSION].
 * @property isSafeMode Whether to enable AES-256-GCM encryption, defaults to false.
 * @property timeFormat Timestamp format string, defaults to "YYYY-MM-DD HH:mm:ss".
 * @property offset Time offset in seconds from server time, defaults to 0.
 */
data class Config(
    val baseUrl: String = Constants.DEFAULT_BASE_URL,
    val appId: Int,
    val apiKey: String,
    val secretKey: String,
    val version: Int = Constants.DEFAULT_VERSION,
    val isSafeMode: Boolean = Constants.DEFAULT_SAFE_MODE,
    val timeFormat: String = Constants.DEFAULT_TIME_FORMAT,
    val offset: Int = Constants.DEFAULT_OFFSET
) {
    init {
        Validation.validateConfig(this)
    }
}

/**
 * Internal mutable config that can be updated after construction
 * (e.g., by [T1YClient.init] which syncs offset and safeMode from the server).
 */
internal data class MutableConfig(
    var baseUrl: String,
    var appId: Int,
    var apiKey: String,
    var secretKey: String,
    var version: Int,
    var isSafeMode: Boolean,
    var timeFormat: String,
    var offset: Int
) {
    constructor(config: Config) : this(
        baseUrl = config.baseUrl,
        appId = config.appId,
        apiKey = config.apiKey,
        secretKey = config.secretKey,
        version = config.version,
        isSafeMode = config.isSafeMode,
        timeFormat = config.timeFormat,
        offset = config.offset
    )
}
