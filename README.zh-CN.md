# t1yOS SDK for Kotlin/Android

[English](./README.md)

[t1yOS](https://www.t1y.net) Serverless 平台 Kotlin/Android SDK — 云数据库、元数据和云函数客户端。

## 特性

- **云数据库** — 完整 CRUD、聚合管道、分页查询、批量操作、Schema 管理
- **云函数** — 调用 `.jsc` 云函数，自动规范化函数名
- **应用元数据** — 获取和检查应用版本及配置信息
- **安全机制** — HMAC-SHA256 请求签名，AES-256-GCM 请求/响应加密（安全模式）
- **特殊类型** — ObjectID、Date、DateTime、Timestamp、数值类型、结构化类型、空值标记、服务端时间助手
- **挂起函数** — Kotlin 协程实现异步，无回调，代码简洁清晰
- **Kotlin + Java 兼容** — Kotlin 原生设计，Java 8+ 可直接调用

## 环境要求

- Kotlin 2.0+
- Java 8+ / Android API 21+
- OkHttp 4.x（自动引入）
- kotlinx.serialization（自动引入）
- kotlinx.coroutines（自动引入）

## 安装

在 `build.gradle.kts` 中添加：

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("net.t1y:t1y-sdk:1.0.0")
}
```

## 快速开始

```kotlin
import com.t1y.sdk.T1YClient
import com.t1y.sdk.Config
import com.t1y.sdk.special.timeNow

// 1. 创建客户端
val client = T1YClient(Config(
    appId = 1001, // 必填：应用 ID（>= 1001）
    apiKey = "4fd7448cdc684431a62d8a0111dc69", // 必填：32 位 API Key
    secretKey = "17b784e359c946ffa65eebbf9ce29", // 必填：32 位 Secret Key
    // 以下参数可选（均有默认值）：
    // baseUrl = "https://myapp.t1y.net",
    // version = 0,
    // isSafeMode = false,
    // timeFormat = "YYYY-MM-DD HH:mm:ss",
    // offset = 0,
))

// 2. 初始化（与服务器同步时间偏移和安全模式）
client.init()

// 3. 开始使用数据库！
client.db.collection("users").insertOne(
    mapOf(
        "name" to "张三",
        "age" to 25,
        "active" to true,
        "customTimeAt" to timeNow.Now()
    )
)
```

## 数据库操作

### 单条操作

```kotlin
val db = client.db.collection("users")

// 插入一条
val insertResult = db.insertOne(mapOf("name" to "张三", "age" to 25))
println(insertResult.data.objectId) // "507f1f77bcf86cd799439011"

// 通过 ObjectID 查询
val findResult = db.findById("507f1f77bcf86cd799439011")
println(findResult.data.result) // { _id: "507f1f77...", name: "张三", ... }

// 通过 ObjectID 更新
db.updateById("507f1f77bcf86cd799439011", mapOf("name" to "张三丰", "age" to 26))

// 通过 ObjectID 删除
db.deleteById("507f1f77bcf86cd799439011")
```

### 条件操作

```kotlin
// 条件查询一条
val user = db.findOne(mapOf("name" to "张三"))

// 条件更新一条
db.updateOne(
    filter = mapOf("name" to "张三"),
    body = mapOf("age" to 27)
)

// 条件删除一条
db.deleteOne(mapOf("name" to "张三"))
```

### 批量操作

```kotlin
// 插入多条
val batchResult = db.insertMany(
    listOf(
        mapOf("name" to "张三", "age" to 25),
        mapOf("name" to "李四", "age" to 30)
    )
)
println(batchResult.data.insertedCount) // 2

// 删除多条
db.deleteMany(mapOf("age" to mapOf("\$lt" to 18)))

// 更新多条
db.updateMany(
    filter = mapOf("status" to "inactive"),
    body = mapOf("status" to "archived")
)
```

### 高级查询

```kotlin
// 分页查询
val pageResult = db.find(
    page = 1,             // 页码（从 1 开始）
    size = 20,            // 每页条数（最大 100）
    sort = mapOf("createdAt" to -1), // 按创建时间倒序
    filter = mapOf("age" to mapOf("\$gte" to 18))
)
println(pageResult.data.results) // 文档数组
println(pageResult.data.pagination) // Pagination(totalItems=42, totalPages=3)

// 聚合查询
val aggResult = db.aggregate(
    listOf(
        mapOf("\$match" to mapOf("status" to "completed")),
        mapOf("\$group" to mapOf("_id" to "\$category", "total" to mapOf("\$sum" to "\$amount"))),
        mapOf("\$sort" to mapOf("total" to -1))
    )
)

// 计数
val countResult = db.count(mapOf("status" to "active"))
println(countResult.data.count)

// 去重查询
val cities = db.distinct("city")
// 带条件过滤
val filteredCities = db.distinct("city", mapOf("country" to "China"))
```

### 表管理

```kotlin
// 获取所有表
val collections = client.db.getCollections()
println(collections.data.results) // ["users", "orders", "products"]

// 创建表
client.db.collection("posts").create()

// 清空表（保留表结构）
val clearResult = client.db.collection("posts").clear()
println(clearResult.data.deletedCount)

// 删除表（删除表结构 + 所有数据）
client.db.collection("posts").drop()
```

## 特殊类型

SDK 提供了一系列辅助函数，用于生成服务端可识别的类型标记。这些标记会在服务端被自动转换为对应的 Go 原生类型。

```kotlin
import com.t1y.sdk.special.*

client.db.collection("users").insertOne(
    mapOf(
        // ObjectID 引用
        "userId" to ObjectID("507f1f77bcf86cd799439011"),

        // 日期类型
        "birthday" to Date("2000-01-01T00:00:00Z"),
        "eventTime" to DateTime("2024-06-15T14:30:00Z"),
        "loginAt" to Timestamp(1705312200L),

        // 数值类型
        "active" to Boolean(true),
        "quantity" to Integer(42),
        "bigNumber" to Bigint(9007199254740991L),
        "rating" to Float(4.5),
        "preciseValue" to Double(3.141592653589793),

        // 结构化类型
        "tags" to Array(listOf("kotlin", "android")),
        "metadata" to Map(mapOf("theme" to "dark", "lang" to "zh")),
        "history" to MapArray(
            listOf(
                mapOf("action" to "login"),
                mapOf("action" to "logout")
            )
        ),

        // 空值（服务端转为 nil）
        "deletedAt" to Null,
        "middleName" to None,

        // 服务端时间辅助
        "customTimeAt" to timeNow.Now(),       // 服务端的 time.Now()
        "unixCreatedAt" to timeNow.NowUnix()   // 服务端的 time.Now().Unix()
    )
)
```

## 元数据

```kotlin
// 获取全部元数据
val meta = client.getMeta()
println(meta.data) // { version: 1, collections: [...], ... }

// 获取指定字段
val versionData = client.getMeta("version")
println(versionData.data) // { result: 1 }

// 检查应用更新
val hasUpdate = client.checkUpdate()
if (hasUpdate) {
    println("有新版本可用！")
}
```

## 云函数

```kotlin
// 调用 .jsc 云函数（扩展名自动规范化）
val result = client.callFunc("hello", mapOf("name" to "World"))
println(result.data)

// 为此调用单独启用安全模式
val safeResult = client.callFunc(
    name = "secureFunc",
    params = mapOf("secret" to "data"),
    enableSafeMode = true
)
```

## 安全机制

### 认证请求头

每个请求都会携带以下请求头：

- `X-T1Y-Application-ID` — 应用 ID
- `X-T1Y-API-Key` — 32 位 API Key
- `X-T1Y-Safe-Timestamp` — Unix 时间戳（UTC + `init()` 获取的时间偏移）
- `X-T1Y-Safe-Sign` — HMAC-SHA256 签名（64 位十六进制字符）

### 签名算法

```
message = METHOD + "\n" + URL_PATH + "\n" + SHA256(body) + "\n" + appId + "\n" + timestamp
signature = HMAC-SHA256(secretKey, message)
```

### 安全模式（AES-256-GCM）

当启用安全模式时（通过配置 `isSafeMode: true` 或 `init()` 自动检测），请求体将使用 AES-256-GCM 加密（密钥为 SecretKey），服务端响应也会自动解密。

```kotlin
// 全局启用安全模式
val client = T1YClient(Config(
    appId = 1001,
    apiKey = "...",
    secretKey = "...",
    isSafeMode = true
))

// 或针对单个请求启用
client.callFunc("secureFunc", params, enableSafeMode = true)
```

## 错误处理

```kotlin
import com.t1y.sdk.exception.T1YException
import com.t1y.sdk.exception.ValidationException

try {
    client.db.collection("users").findById("invalid-id")
} catch (e: T1YException) {
    // 服务器 API 错误或网络失败
    println("错误 ${e.code}: ${e.message}")
} catch (e: ValidationException) {
    // 客户端参数验证错误
    println("参数验证失败: ${e.message}")
}
```

## 响应格式

所有 API 响应均遵循 `ApiResponse<T>` 格式：

```kotlin
data class ApiResponse<T>(
    val code: Int,       // 0 = 成功
    val message: String, // 可读消息
    val data: T          // 响应数据
)
```

预定义结果类型：

| 类型                | 使用场景                                                 |
| ------------------- | -------------------------------------------------------- |
| `InsertResult`      | `insertOne` — 返回 `objectId`                            |
| `InsertManyResult`  | `insertMany` — 返回 `objectIds`, `insertedCount`         |
| `DeleteResult`      | `deleteById`、`deleteOne`、`clear` — 返回 `deletedCount` |
| `DeleteManyResult`  | `deleteMany` — 返回 `deletedCount`                       |
| `UpdateResult`      | `updateById`、`updateOne` — 返回 `modifiedCount`         |
| `UpdateManyResult`  | `updateMany` — 返回 `modifiedCount`                      |
| `FindResult`        | `findById`、`findOne` — 返回 `result`                    |
| `PaginationResult`  | `find` — 返回 `results`、`page`、`size`、`pagination`    |
| `AggregateResult`   | `aggregate` — 返回 `results`                             |
| `CountResult`       | `count` — 返回 `count`                                   |
| `CollectionsResult` | `db.getCollections()` — 返回 `results`                   |

## API 参考

### T1YClient

主客户端类（别名：`T1YOS`）。

| 方法                                             | 说明                                        |
| ------------------------------------------------ | ------------------------------------------- |
| `T1YClient(config)`                              | 创建客户端（校验 appId、apiKey、secretKey） |
| `init()`                                         | 与服务端同步时间偏移和安全模式              |
| `getMeta(field?)`                                | 获取应用元数据                              |
| `checkUpdate()`                                  | 检查服务端是否存在新版本                    |
| `callFunc(name, params?, enableSafeMode?)`       | 调用云函数                                  |
| `request<T>(method, path, params?, encryption?)` | 类型化认证请求                              |
| `requestRaw(method, path, params?, encryption?)` | 原始认证请求（动态响应）                    |
| `db.collection(name)`                            | 获取集合操作实例（链式调用）                |
| `db.toObjectID(id)`                              | 创建 ObjectID 标记字符串                    |
| `db.getCollections()`                            | 获取所有表                                  |
| `assertObjectID(idStr)`                          | 校验 24 位十六进制 ObjectID                 |
| `hmacSHA256(secret, message)`                    | 计算 HMAC-SHA256 十六进制摘要               |
| `verifyHmacSHA256(secret, message, sig)`         | 校验 HMAC-SHA256 签名                       |

### T1Collection

数据库集合，提供链式 CRUD 操作。

| 方法                             | HTTP   | 端点                                |
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

## 开源协议

[MIT](./LICENSE)

Copyright (c) 2026 华易云联（杭州）网络科技有限责任公司
