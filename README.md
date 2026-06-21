# t1yOS SDK for Kotlin/Android

[中文文档](./README.zh-CN.md)

[t1yOS](https://www.t1y.net) Serverless Platform Kotlin/Android SDK — cloud database, metadata, and cloud functions client.

## Features

- **Cloud Database** — Full CRUD, aggregation pipelines, paginated queries, bulk operations, schema management
- **Cloud Functions** — Call `.jsc` cloud functions with automatic name normalization
- **Application Metadata** — Retrieve and check application version and configuration
- **Security** — HMAC-SHA256 request signing, AES-256-GCM request/response encryption (safe mode)
- **Special Types** — ObjectID, Date, DateTime, Timestamp, numeric types, structured types, null markers, server-side time helpers
- **Suspend Functions** — Kotlin coroutines for clean async code, no callbacks
- **Kotlin + Java Compatible** — Designed in Kotlin, callable from Java 8+

## Requirements

- Kotlin 2.0+
- Java 8+ / Android API 21+
- OkHttp 4.x (included as dependency)
- kotlinx.serialization (included as dependency)
- kotlinx.coroutines (included as dependency)

## Installation

Add to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("net.t1y:t1y-sdk:1.0.0")
}
```

## Quick Start

```kotlin
import com.t1y.sdk.T1YClient
import com.t1y.sdk.Config
import com.t1y.sdk.special.timeNow

// 1. Create client
val client = T1YClient(Config(
    appId = 1001, // Required: your application ID (>= 1001)
    apiKey = "4fd7448cdc684431a62d8a0111dc69", // Required: 32-character API Key
    secretKey = "17b784e359c946ffa65eebbf9ce29", // Required: 32-character Secret Key
    // Optional with defaults:
    // baseUrl = "https://myapp.t1y.net",
    // version = 0,
    // isSafeMode = false,
    // timeFormat = "YYYY-MM-DD HH:mm:ss",
    // offset = 0,
))

// 2. Initialize (syncs time offset and safe mode with server)
client.init()

// 3. Use the database!
client.db.collection("users").insertOne(
    mapOf(
        "name" to "Alice",
        "age" to 25,
        "active" to true,
        "customTimeAt" to timeNow.Now()
    )
)
```

## Database Operations

### Single Document

```kotlin
val db = client.db.collection("users")

// Insert one
val insertResult = db.insertOne(mapOf("name" to "Alice", "age" to 25))
println(insertResult.data.objectId) // "507f1f77bcf86cd799439011"

// Find by ObjectID
val findResult = db.findById("507f1f77bcf86cd799439011")
println(findResult.data.result) // { _id: "507f1f77...", name: "Alice", ... }

// Update by ObjectID
db.updateById("507f1f77bcf86cd799439011", mapOf("name" to "Alice Smith", "age" to 26))

// Delete by ObjectID
db.deleteById("507f1f77bcf86cd799439011")
```

### Filter-based Operations

```kotlin
// Find one by filter
val user = db.findOne(mapOf("name" to "Alice"))

// Update one by filter
db.updateOne(
    filter = mapOf("name" to "Alice"),
    body = mapOf("age" to 27)
)

// Delete one by filter
db.deleteOne(mapOf("name" to "Alice"))
```

### Bulk Operations

```kotlin
// Insert many
val batchResult = db.insertMany(
    listOf(
        mapOf("name" to "Alice", "age" to 25),
        mapOf("name" to "Bob", "age" to 30)
    )
)
println(batchResult.data.insertedCount) // 2

// Delete many
db.deleteMany(mapOf("age" to mapOf("\$lt" to 18)))

// Update many
db.updateMany(
    filter = mapOf("status" to "inactive"),
    body = mapOf("status" to "archived")
)
```

### Advanced Queries

```kotlin
// Paginated find
val pageResult = db.find(
    page = 1,             // page number (1-based)
    size = 20,            // page size (max 100)
    sort = mapOf("createdAt" to -1), // newest first
    filter = mapOf("age" to mapOf("\$gte" to 18))
)
println(pageResult.data.results) // List of documents
println(pageResult.data.pagination) // Pagination(totalItems=42, totalPages=3)

// Aggregation pipeline
val aggResult = db.aggregate(
    listOf(
        mapOf("\$match" to mapOf("status" to "completed")),
        mapOf("\$group" to mapOf("_id" to "\$category", "total" to mapOf("\$sum" to "\$amount"))),
        mapOf("\$sort" to mapOf("total" to -1))
    )
)

// Count
val countResult = db.count(mapOf("status" to "active"))
println(countResult.data.count)

// Distinct values
val cities = db.distinct("city")
// With filter
val filteredCities = db.distinct("city", mapOf("country" to "China"))
```

### Schema Management

```kotlin
// Get all collections
val collections = client.db.getCollections()
println(collections.data.results) // ["users", "orders", "products"]

// Create a collection
client.db.collection("posts").create()

// Clear a collection (truncate all documents)
val clearResult = client.db.collection("posts").clear()
println(clearResult.data.deletedCount)

// Drop a collection (delete schema + all documents)
client.db.collection("posts").drop()
```

## Special Types

The SDK provides helper functions that produce server-recognized type markers.
These markers are automatically converted to native Go types on the server side.

```kotlin
import com.t1y.sdk.special.*

client.db.collection("users").insertOne(
    mapOf(
        // ObjectID reference
        "userId" to ObjectID("507f1f77bcf86cd799439011"),

        // Date types
        "birthday" to Date("2000-01-01T00:00:00Z"),
        "eventTime" to DateTime("2024-06-15T14:30:00Z"),
        "loginAt" to Timestamp(1705312200L),

        // Numeric types
        "active" to Boolean(true),
        "quantity" to Integer(42),
        "bigNumber" to Bigint(9007199254740991L),
        "rating" to Float(4.5),
        "preciseValue" to Double(3.141592653589793),

        // Structured types
        "tags" to Array(listOf("kotlin", "android")),
        "metadata" to Map(mapOf("theme" to "dark", "lang" to "en")),
        "history" to MapArray(
            listOf(
                mapOf("action" to "login"),
                mapOf("action" to "logout")
            )
        ),

        // Null values (server converts to nil)
        "deletedAt" to Null,
        "middleName" to None,

        // Server-side time helpers
        "customTimeAt" to timeNow.Now(),       // server's time.Now()
        "unixCreatedAt" to timeNow.NowUnix()   // server's time.Now().Unix()
    )
)
```

## Metadata

```kotlin
// Get all metadata
val meta = client.getMeta()
println(meta.data) // { version: 1, collections: [...], ... }

// Get a specific field
val versionData = client.getMeta("version")
println(versionData.data) // { result: 1 }

// Check for application updates
val hasUpdate = client.checkUpdate()
if (hasUpdate) {
    println("A new version is available!")
}
```

## Cloud Functions

```kotlin
// Call a .jsc cloud function (extension auto-normalized)
val result = client.callFunc("hello", mapOf("name" to "World"))
println(result.data)

// With safe mode enabled for this specific call
val safeResult = client.callFunc(
    name = "secureFunc",
    params = mapOf("secret" to "data"),
    enableSafeMode = true
)
```

## Security

### Authentication Headers

Every request includes the following headers:

- `X-T1Y-Application-ID` — Your application ID
- `X-T1Y-API-Key` — Your 32-character API key
- `X-T1Y-Safe-Timestamp` — Unix timestamp (UTC + time offset from `init()`)
- `X-T1Y-Safe-Sign` — HMAC-SHA256 signature (64 hex characters)

### Signature Algorithm

```
message = METHOD + "\n" + URL_PATH + "\n" + SHA256(body) + "\n" + appId + "\n" + timestamp
signature = HMAC-SHA256(secretKey, message)
```

### Safe Mode (AES-256-GCM)

When safe mode is enabled (via `isSafeMode: true` in config or auto-detected from `init()`), request bodies are encrypted with AES-256-GCM using your SecretKey, and server responses are automatically decrypted.

```kotlin
// Enable safe mode globally
val client = T1YClient(Config(
    appId = 1001,
    apiKey = "...",
    secretKey = "...",
    isSafeMode = true
))

// Or enable for a single request
client.callFunc("secureFunc", params, enableSafeMode = true)
```

## Error Handling

```kotlin
import com.t1y.sdk.exception.T1YException
import com.t1y.sdk.exception.ValidationException

try {
    client.db.collection("users").findById("invalid-id")
} catch (e: T1YException) {
    // Server API error or network failure
    println("Error ${e.code}: ${e.message}")
} catch (e: ValidationException) {
    // Client-side input validation error
    println("Validation failed: ${e.message}")
}
```

## Response Format

All API responses follow the `ApiResponse<T>` format:

```kotlin
data class ApiResponse<T>(
    val code: Int,       // 0 = success
    val message: String, // Human-readable message
    val data: T          // Response payload
)
```

Pre-defined result types:

| Type                | Used By                                                     |
| ------------------- | ----------------------------------------------------------- |
| `InsertResult`      | `insertOne` — returns `objectId`                            |
| `InsertManyResult`  | `insertMany` — returns `objectIds`, `insertedCount`         |
| `DeleteResult`      | `deleteById`, `deleteOne`, `clear` — returns `deletedCount` |
| `DeleteManyResult`  | `deleteMany` — returns `deletedCount`                       |
| `UpdateResult`      | `updateById`, `updateOne` — returns `modifiedCount`         |
| `UpdateManyResult`  | `updateMany` — returns `modifiedCount`                      |
| `FindResult`        | `findById`, `findOne` — returns `result`                    |
| `PaginationResult`  | `find` — returns `results`, `page`, `size`, `pagination`    |
| `AggregateResult`   | `aggregate` — returns `results`                             |
| `CountResult`       | `count` — returns `count`                                   |
| `CollectionsResult` | `db.getCollections()` — returns `results`                   |

## API Reference

### T1YClient

The main client class (alias: `T1YOS`).

| Method                                           | Description                                        |
| ------------------------------------------------ | -------------------------------------------------- |
| `T1YClient(config)`                              | Create client (validates appId, apiKey, secretKey) |
| `init()`                                         | Sync time offset and safe mode with server         |
| `getMeta(field?)`                                | Get application metadata                           |
| `checkUpdate()`                                  | Check if newer version exists on server            |
| `callFunc(name, params?, enableSafeMode?)`       | Call a cloud function                              |
| `request<T>(method, path, params?, encryption?)` | Typed authenticated request                        |
| `requestRaw(method, path, params?, encryption?)` | Raw authenticated request (dynamic response)       |
| `db.collection(name)`                            | Get a collection instance (chainable)              |
| `db.toObjectID(id)`                              | Create ObjectID marker string                      |
| `db.getCollections()`                            | List all collections                               |
| `assertObjectID(idStr)`                          | Validate 24-char hex ObjectID                      |
| `hmacSHA256(secret, message)`                    | Compute HMAC-SHA256 hex digest                     |
| `verifyHmacSHA256(secret, message, sig)`         | Verify HMAC-SHA256 signature                       |

### T1Collection

Database collection providing chainable CRUD operations.

| Method                           | HTTP   | Endpoint                            |
| -------------------------------- | ------ | ----------------------------------- |
| `insertOne(data)`                | POST   | `/v5/classes/:name`                 |
| `deleteById(objectId)`           | DELETE | `/v5/classes/:name/:objectId`       |
| `updateById(objectId, data)`     | PUT    | `/v5/classes/:name/:objectId`       |
| `findById(objectId)`             | GET    | `/v5/classes/:name/:objectId`       |
| `deleteOne(filter)`              | DELETE | `/v5/classes/:name/one`             |
| `updateOne(filter, body)`        | PUT    | `/v5/classes/:name/one`             |
| `findOne(filter)`                | POST   | `/v5/classes/:name/one`             |
| `insertMany(dataList)`           | POST   | `/v5/classes/:name/many`            |
| `deleteMany(filter)`             | DELETE | `/v5/classes/:name/many`            |
| `updateMany(filter, body)`       | PUT    | `/v5/classes/:name/many`            |
| `find(page, size, sort, filter)` | POST   | `/v5/classes/:name/find`            |
| `aggregate(pipeline)`            | POST   | `/v5/classes/:name/aggregate`       |
| `count(filter?)`                 | POST   | `/v5/classes/:name/count`           |
| `distinct(fieldName, filter?)`   | POST   | `/v5/classes/:name/distinct/:field` |
| `create()`                       | POST   | `/v5/schemas/:name`                 |
| `clear()`                        | PUT    | `/v5/schemas/:name`                 |
| `drop()`                         | DELETE | `/v5/schemas/:name`                 |

## License

[MIT](./LICENSE)

Copyright (c) 2026 华易云联（杭州）网络科技有限责任公司
