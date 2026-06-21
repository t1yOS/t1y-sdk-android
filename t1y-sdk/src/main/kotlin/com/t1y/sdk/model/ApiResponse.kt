package com.t1y.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Standard API response wrapper for all t1yOS server responses.
 *
 * @param T The type of the `data` field.
 * @property code Response status code (0 indicates success).
 * @property message Human-readable response message.
 * @property data Response payload.
 */
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T
)

// ===== Single Document Result Types =====

/** Result of [com.t1y.sdk.T1Collection.insertOne]. */
@Serializable
data class InsertResult(
    val objectId: String
)

/** Result of [com.t1y.sdk.T1Collection.insertMany]. */
@Serializable
data class InsertManyResult(
    val objectIds: List<String>,
    val insertedCount: Int
)

/** Result of single document delete operations. */
@Serializable
data class DeleteResult(
    val deletedCount: Int
)

/** Result of bulk delete operations. */
@Serializable
data class DeleteManyResult(
    val deletedCount: Int
)

/** Result of single document update operations. */
@Serializable
data class UpdateResult(
    val modifiedCount: Int
)

/** Result of bulk update operations. */
@Serializable
data class UpdateManyResult(
    val modifiedCount: Int
)

/** Result of find one / find by ID operations. */
@Serializable
data class FindResult(
    val result: JsonElement? = null
)

// ===== Pagination =====

/** Pagination metadata. */
@Serializable
data class Pagination(
    val totalItems: Int,
    val totalPages: Int
)

/** Result of paginated [com.t1y.sdk.T1Collection.find] queries. */
@Serializable
data class PaginationResult(
    val results: List<JsonElement>,
    val page: Int,
    val size: Int,
    val pagination: Pagination
)

// ===== Aggregate / Query =====

/** Result of [com.t1y.sdk.T1Collection.aggregate] pipeline queries. */
@Serializable
data class AggregateResult(
    val results: List<JsonElement>
)

// ===== Initialization =====

/** Result of the init endpoint. */
@Serializable
data class InitResult(
    val unix: Long,
    @SerialName("is_safe_mode")
    val isSafeMode: Boolean
)

// ===== Count =====

/** Result of [com.t1y.sdk.T1Collection.count] queries. */
@Serializable
data class CountResult(
    val count: Int
)

// ===== Collections =====

/** Result of listing all collections. */
@Serializable
data class CollectionsResult(
    val results: List<String>
)
