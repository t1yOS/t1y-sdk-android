package com.t1y.sdk.network

import com.t1y.sdk.Constants
import com.t1y.sdk.exception.T1YException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Creates the OkHttp client instance used by the SDK.
 * Configures timeouts and connection pooling suitable for mobile usage.
 */
internal fun createOkHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(Constants.REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

/**
 * Executes an OkHttp request as a Kotlin suspend function.
 *
 * Uses [Dispatchers.IO] + blocking [okhttp3.Call.execute] for simplicity
 * and reliability across all JVM/Android versions. OkHttp's connection
 * pooling and retry logic work correctly with this approach.
 *
 * @return The OkHttp [Response].
 * @throws T1YException on network errors (code=0) or timeouts (code=408).
 */
internal suspend fun OkHttpClient.executeSuspend(request: Request): Response =
    withContext(Dispatchers.IO) {
        try {
            newCall(request).execute()
        } catch (e: IOException) {
            val message = e.message ?: "Network error"
            val isTimeout = message.contains("timeout", ignoreCase = true) ||
                    message.contains("timed out", ignoreCase = true)
            throw T1YException(
                code = if (isTimeout) 408 else 0,
                message = message
            )
        }
    }
