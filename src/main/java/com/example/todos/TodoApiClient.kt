package com.example.todos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class TodoApiClient {
    private val log = LoggerFactory.getLogger(TodoApiClient::class.java)

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    private val client = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://beta.mrdekk.ru/todo/"
    private val bearerToken = "6bbf91fe-9f2a-419a-a604-45b2148b11e0"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private var currentRevision: Int = 0

    suspend fun loadTodos(): List<NetworkTodoItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${baseUrl}list")
            .addHeader("Authorization", "Bearer $bearerToken")
            .get()
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "No error body"
                    log.error("HTTP ${response.code}: $errorBody")
                    throw IOException("HTTP ${response.code}: $errorBody")
                }

                val json = response.body?.string() ?: throw IOException("Empty response")
                log.debug("Response: $json")

                val obj = JSONObject(json)

                if (obj.getString("status") != "ok") throw IOException("API error")

                currentRevision = obj.getInt("revision")
                val list = obj.getJSONArray("list")

                parseTodoList(list)
            }
        } catch (e: Exception) {
            log.error("Failed to load todos: ${e.message}")
            emptyList()
        }
    }

    suspend fun addTodo(todo: NetworkTodoItem): Boolean = withContext(Dispatchers.IO) {
        // Создаем JSON с ключом "element"
        val elementJson = JSONObject().apply {
            put("id", todo.id)
            put("text", todo.text)
            put("importance", todo.importance)
            todo.deadline?.let { put("deadline", it) }
            put("done", todo.done)
            todo.color?.let { put("color", it) }
            put("created_at", todo.createdAt)
            put("changed_at", todo.changedAt)
            put("last_updated_by", todo.lastUpdatedBy)
        }

        val json = JSONObject().apply {
            put("element", elementJson)
        }

        log.debug("=== ADD TODO REQUEST ===")
        log.debug("Revision: $currentRevision")
        log.debug("JSON: ${json.toString(2)}")

        val requestBody = RequestBody.create(jsonMediaType, json.toString())
        val request = Request.Builder()
            .url("${baseUrl}list")
            .addHeader("Authorization", "Bearer $bearerToken")
            .addHeader("X-Last-Known-Revision", currentRevision.toString())
            .post(requestBody)
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                log.debug("=== ADD TODO RESPONSE ===")
                log.debug("Status: ${response.code}")
                log.debug("Body: $responseBody")

                if (!response.isSuccessful) {
                    log.error("HTTP ${response.code}: $responseBody")
                    return@use false
                }

                val obj = JSONObject(responseBody)
                if (obj.getString("status") == "ok") {
                    currentRevision = obj.getInt("revision")
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            log.error("Failed to add todo: ${e.message}")
            false
        }
    }

    suspend fun updateTodo(todo: NetworkTodoItem): Boolean = withContext(Dispatchers.IO) {
        val elementJson = JSONObject().apply {
            put("id", todo.id)
            put("text", todo.text)
            put("importance", todo.importance)
            todo.deadline?.let { put("deadline", it) }
            put("done", todo.done)
            todo.color?.let { put("color", it) }
            put("created_at", todo.createdAt)
            put("changed_at", todo.changedAt)
            put("last_updated_by", todo.lastUpdatedBy)
        }

        val json = JSONObject().apply {
            put("element", elementJson)
        }

        val requestBody = RequestBody.create(jsonMediaType, json.toString())
        val request = Request.Builder()
            .url("${baseUrl}list/${todo.id}")
            .addHeader("Authorization", "Bearer $bearerToken")
            .addHeader("X-Last-Known-Revision", currentRevision.toString())
            .put(requestBody)
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                handleResponse(response, "update")
            }
        } catch (e: Exception) {
            log.error("Failed to update todo: ${e.message}")
            false
        }
    }

    suspend fun deleteTodo(id: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${baseUrl}list/$id")
            .addHeader("Authorization", "Bearer $bearerToken")
            .addHeader("X-Last-Known-Revision", currentRevision.toString())
            .delete()
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                handleResponse(response, "delete")
            }
        } catch (e: Exception) {
            log.error("Failed to delete todo: ${e.message}")
            false
        }
    }

    private fun handleResponse(response: Response, operation: String): Boolean {
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            log.error("HTTP ${response.code} when $operation todo: $responseBody")
            return false
        }

        val obj = JSONObject(responseBody)

        if (obj.getString("status") != "ok") return false

        currentRevision = obj.getInt("revision")
        return true
    }

    private fun parseTodoList(jsonArray: JSONArray): List<NetworkTodoItem> {
        val items = mutableListOf<NetworkTodoItem>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            items.add(NetworkTodoItem(
                id = item.getString("id"),
                text = item.getString("text"),
                importance = item.getString("importance"),
                deadline = if (item.has("deadline") && !item.isNull("deadline")) {
                    item.getLong("deadline")
                } else null,
                done = item.getBoolean("done"),
                color = if (item.has("color") && !item.isNull("color")) {
                    item.getString("color")
                } else null,
                createdAt = item.getLong("created_at"),
                changedAt = item.getLong("changed_at"),
                lastUpdatedBy = item.getString("last_updated_by")
            ))
        }

        return items
    }
}