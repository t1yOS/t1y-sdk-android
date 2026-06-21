package com.t1y.sdk

import com.t1y.sdk.model.*
import com.t1y.sdk.util.Validation
import kotlinx.serialization.json.JsonElement

/**
 * Database collection providing chainable CRUD and schema operations.
 *
 * Created via [T1YClient.DatabaseAccessor.collection] — never instantiated directly.
 *
 * ## Usage
 * ```
 * val col = client.db.collection("users")
 * col.insertOne(mapOf("name" to "Alice", "age" to 25))
 * col.findById("507f1f77bcf86cd799439011")
 * col.find(page = 1, size = 10)
 * ```
 *
 * @property name The collection name.
 */
class T1Collection internal constructor(
    private val client: T1YClient,
    val name: String
) {
    private val encodedName = java.net.URLEncoder.encode(name, "UTF-8")

    // ===== Single Document Operations =====

    /**
     * Inserts a single document into the collection.
     *
     * @param data Document data as a Map.
     * @return [ApiResponse] with [InsertResult] containing the inserted document's ObjectID.
     */
    suspend fun insertOne(data: Map<String, Any?>): ApiResponse<InsertResult> {
        Validation.requireNonEmptyObject(data, "insertOne data")
        return client.request("POST", "/${Constants.API_VERSION}/classes/$encodedName", data)
    }

    /**
     * Deletes a document by its ObjectID.
     *
     * @param objectId 24-character hex ObjectID string.
     * @return [ApiResponse] with [DeleteResult].
     */
    suspend fun deleteById(objectId: String): ApiResponse<DeleteResult> {
        Validation.assertObjectID(objectId)
        return client.request(
            "DELETE",
            "/${Constants.API_VERSION}/classes/$encodedName/$objectId"
        )
    }

    /**
     * Updates a document by its ObjectID.
     *
     * @param objectId 24-character hex ObjectID string.
     * @param data Update data as a Map (only specified fields are updated).
     * @return [ApiResponse] with [UpdateResult].
     */
    suspend fun updateById(
        objectId: String,
        data: Map<String, Any?>
    ): ApiResponse<UpdateResult> {
        Validation.assertObjectID(objectId)
        Validation.requireNonEmptyObject(data, "updateById data")
        return client.request(
            "PUT",
            "/${Constants.API_VERSION}/classes/$encodedName/$objectId",
            data
        )
    }

    /**
     * Finds a document by its ObjectID.
     *
     * @param objectId 24-character hex ObjectID string.
     * @return [ApiResponse] with [FindResult].
     */
    suspend fun findById(objectId: String): ApiResponse<FindResult> {
        Validation.assertObjectID(objectId)
        return client.request(
            "GET",
            "/${Constants.API_VERSION}/classes/$encodedName/$objectId"
        )
    }

    // ===== Filter-based Single Operations =====

    /**
     * Deletes the first document matching the filter.
     *
     * @param filter Query filter (non-empty Map).
     * @return [ApiResponse] with [DeleteResult].
     */
    suspend fun deleteOne(filter: Map<String, Any?>): ApiResponse<DeleteResult> {
        Validation.requireNonEmptyObject(filter, "deleteOne filter")
        return client.request(
            "DELETE",
            "/${Constants.API_VERSION}/classes/$encodedName/one",
            filter
        )
    }

    /**
     * Updates the first document matching the filter.
     *
     * @param filter Query filter (non-empty Map).
     * @param body Update data (non-empty Map).
     * @return [ApiResponse] with [UpdateResult].
     */
    suspend fun updateOne(
        filter: Map<String, Any?>,
        body: Map<String, Any?>
    ): ApiResponse<UpdateResult> {
        Validation.requireNonEmptyObject(filter, "updateOne filter")
        Validation.requireNonEmptyObject(body, "updateOne body")
        return client.request(
            "PUT",
            "/${Constants.API_VERSION}/classes/$encodedName/one",
            mapOf("filter" to filter, "body" to body)
        )
    }

    /**
     * Finds the first document matching the filter.
     *
     * @param filter Query filter (non-empty Map).
     * @return [ApiResponse] with [FindResult].
     */
    suspend fun findOne(filter: Map<String, Any?>): ApiResponse<FindResult> {
        Validation.requireNonEmptyObject(filter, "findOne filter")
        return client.request(
            "POST",
            "/${Constants.API_VERSION}/classes/$encodedName/one",
            filter
        )
    }

    // ===== Bulk Operations =====

    /**
     * Inserts multiple documents in a single operation.
     *
     * @param dataList Non-empty list of non-empty document Maps.
     * @return [ApiResponse] with [InsertManyResult].
     */
    suspend fun insertMany(
        dataList: List<Map<String, Any?>>
    ): ApiResponse<InsertManyResult> {
        Validation.requireNonEmptyArrayWithNonEmptyObjects(dataList, "insertMany dataList")
        return client.request(
            "POST",
            "/${Constants.API_VERSION}/classes/$encodedName/many",
            dataList
        )
    }

    /**
     * Deletes all documents matching the filter.
     *
     * @param filter Query filter (non-null Map, can be empty to match all).
     * @return [ApiResponse] with [DeleteManyResult].
     */
    suspend fun deleteMany(filter: Map<String, Any?>): ApiResponse<DeleteManyResult> {
        Validation.requirePlainObject(filter, "deleteMany filter")
        return client.request(
            "DELETE",
            "/${Constants.API_VERSION}/classes/$encodedName/many",
            filter
        )
    }

    /**
     * Updates all documents matching the filter.
     *
     * @param filter Query filter (non-null Map, can be empty to match all).
     * @param body Update data (non-empty Map).
     * @return [ApiResponse] with [UpdateManyResult].
     */
    suspend fun updateMany(
        filter: Map<String, Any?>,
        body: Map<String, Any?>
    ): ApiResponse<UpdateManyResult> {
        Validation.requirePlainObject(filter, "updateMany filter")
        Validation.requireNonEmptyObject(body, "updateMany body")
        return client.request(
            "PUT",
            "/${Constants.API_VERSION}/classes/$encodedName/many",
            mapOf("filter" to filter, "body" to body)
        )
    }

    // ===== Advanced Queries =====

    /**
     * Finds documents with pagination, sorting, and filtering.
     *
     * @param page Page number (1-based), defaults to 1.
     * @param size Page size, clamped to [Constants.MAX_PAGE_SIZE] (100), defaults to 10.
     * @param sort Sort specification (e.g., `mapOf("createdAt" to -1)`), defaults to descending by createdAt.
     * @param filter Query filter.
     * @return [ApiResponse] with [PaginationResult].
     */
    suspend fun find(
        page: Int = 1,
        size: Int = Constants.DEFAULT_PAGE_SIZE,
        sort: Map<String, Int> = mapOf("createdAt" to -1),
        filter: Map<String, Any?> = emptyMap()
    ): ApiResponse<PaginationResult> {
        require(page >= 1) { "find page must be positive, got: $page" }
        require(size >= 1) { "find size must be positive, got: $size" }
        val effectiveSize = minOf(size, Constants.MAX_PAGE_SIZE)
        return client.request(
            "POST",
            "/${Constants.API_VERSION}/classes/$encodedName/find",
            mapOf(
                "page" to page,
                "size" to effectiveSize,
                "sort" to sort,
                "filter" to filter
            )
        )
    }

    /**
     * Executes a MongoDB-style aggregation pipeline.
     *
     * @param pipeline List of aggregation stage Maps.
     * @return [ApiResponse] with [AggregateResult].
     */
    suspend fun aggregate(
        pipeline: List<Map<String, Any?>>
    ): ApiResponse<AggregateResult> =
        client.request(
            "POST",
            "/${Constants.API_VERSION}/classes/$encodedName/aggregate",
            pipeline
        )

    /**
     * Counts documents matching the filter.
     *
     * @param filter Query filter (can be empty to count all documents).
     * @return [ApiResponse] with [CountResult].
     */
    suspend fun count(
        filter: Map<String, Any?> = emptyMap()
    ): ApiResponse<CountResult> {
        Validation.requirePlainObject(filter, "count filter")
        return client.request(
            "POST",
            "/${Constants.API_VERSION}/classes/$encodedName/count",
            filter
        )
    }

    /**
     * Returns distinct values for a given field.
     *
     * @param fieldName The field to get distinct values for.
     * @param filter Optional query filter.
     * @return [ApiResponse] with raw [JsonElement] data (dynamic result type).
     */
    suspend fun distinct(
        fieldName: String,
        filter: Map<String, Any?> = emptyMap()
    ): ApiResponse<JsonElement> {
        require(fieldName.isNotBlank()) {
            "distinct fieldName must not be blank"
        }
        val encodedField = java.net.URLEncoder.encode(fieldName, "UTF-8")
        return client.requestRaw(
            "POST",
            "/${Constants.API_VERSION}/classes/$encodedName/distinct/$encodedField",
            filter
        )
    }

    // ===== Schema Management =====

    /**
     * Creates the collection schema on the server.
     *
     * @return [ApiResponse] with raw [JsonElement] data.
     */
    suspend fun create(): ApiResponse<JsonElement> =
        client.requestRaw(
            "POST",
            "/${Constants.API_VERSION}/schemas/$encodedName"
        )

    /**
     * Clears all documents from the collection (truncates).
     *
     * @return [ApiResponse] with [DeleteResult] containing the number of deleted documents.
     */
    suspend fun clear(): ApiResponse<DeleteResult> =
        client.request(
            "PUT",
            "/${Constants.API_VERSION}/schemas/$encodedName"
        )

    /**
     * Drops (deletes) the entire collection including its schema.
     *
     * @return [ApiResponse] with raw [JsonElement] data.
     */
    suspend fun drop(): ApiResponse<JsonElement> =
        client.requestRaw(
            "DELETE",
            "/${Constants.API_VERSION}/schemas/$encodedName"
        )
}
