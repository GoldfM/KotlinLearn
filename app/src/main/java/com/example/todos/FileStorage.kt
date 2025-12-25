package com.example.myapplication

import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.slf4j.LoggerFactory

class FileStorage(private val storageFile: File) {
    private val log = LoggerFactory.getLogger(FileStorage::class.java)
    private val _items: MutableList<TodoItem> = mutableListOf()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val items: List<TodoItem>
        get() = _items.toList()

    fun add(item: TodoItem) {
        log.info(
            "Добавление задачи: {} (приоритет: {}, дедлайн: {}, uid: {})",
            item.text,
            item.priority,
            item.deadline?.let { dateFormat.format(it) } ?: "нет",
            item.uid
        )
        _items.add(item)
        log.debug("Всего задач после добавления: {}", _items.size)
    }

    fun remove(uid: String): Boolean {
        log.info("Попытка удаления задачи с uid: {}", uid)
        val itemToRemove = _items.find { it.uid == uid }
        return if (itemToRemove != null) {
            log.info("Удаление задачи: '{}' (uid: {})", itemToRemove.text, uid)
            _items.removeIf { it.uid == uid }
            log.debug("Всего задач после удаления: {}", _items.size)
            true
        } else {
            log.warn("Задача с uid {} не найдена для удаления", uid)
            false
        }
    }

    fun saveToFile() {
        log.info("Сохранение задач в файл: {}", storageFile.absolutePath)
        val jsonItems = JSONArray()
        items.forEach { item ->
            jsonItems.put(item.toJson(dateFormat))
        }

        try {
            storageFile.writeText(jsonItems.toString())
            log.info("Успешно сохранено задач в файл: {}", items.size)
            log.debug("Файл сохранен по пути: {}", storageFile.absolutePath)
        } catch (e: Exception) {
            log.error("Ошибка при сохранении в файл: {}", e.message)
        }
    }

    fun loadFromFile() {
        log.info("Загрузка задач из файла: {}", storageFile.absolutePath)

        if (!storageFile.exists()) {
            log.warn("Файл не существует: {}", storageFile.absolutePath)
            return
        }

        try {
            val jsonString = storageFile.readText()
            val jsonArray = JSONArray(jsonString)
            log.debug("Прочитано из файла: {} символов", jsonString.length)

            _items.clear()
            var loadedCount = 0

            for (i in 0 until jsonArray.length()) {
                try {
                    val jsonItem = jsonArray.getJSONObject(i)
                    val newItem = parseTodoItem(jsonItem, dateFormat)
                    if (newItem != null) {
                        _items.add(newItem)
                        loadedCount++
                        log.debug("Загружена задача: '{}'", newItem.text)
                    }
                } catch (e: Exception) {
                    log.error("Ошибка парсинга элемента {}: {}", i, e.message)
                }
            }

            log.info("Успешно загружено задач из файла: {}", loadedCount)

        } catch (e: Exception) {
            log.error("Ошибка при загрузке из файла: {}", e.message)
        }
    }

    fun updateItem(updatedItem: TodoItem): Boolean {
        val index = _items.indexOfFirst { it.uid == updatedItem.uid }
        return if (index != -1) {
            _items[index] = updatedItem
            log.info("Обновлена задача: {} (uid: {})", updatedItem.text, updatedItem.uid)
            true
        } else {
            log.warn("Задача с uid {} не найдена для обновления", updatedItem.uid)
            false
        }
    }

    // Новый метод для получения задачи по ID
    fun getItemById(uid: String): TodoItem? {
        return _items.find { it.uid == uid }
    }
}

// Функция-расширение для TodoItem
fun TodoItem.toJson(dateFormat: SimpleDateFormat): org.json.JSONObject {
    return org.json.JSONObject().apply {
        put("uid", uid)
        put("text", text)
        put("priority", priority.name)
        put("isDone", isDone)
        put("color", color.value.toLong())

        deadline?.let {
            put("deadline", dateFormat.format(it))
        }
    }
}

// Отдельная функция для парсинга
fun parseTodoItem(json: org.json.JSONObject, dateFormat: SimpleDateFormat): TodoItem? {
    return try {
        TodoItem(
            uid = json.getString("uid"),
            text = json.getString("text"),
            priority = Priority.valueOf(json.getString("priority")),
            isDone = json.getBoolean("isDone"),
            color = androidx.compose.ui.graphics.Color(json.getLong("color").toULong()),
            deadline = if (json.has("deadline")) {
                try {
                    dateFormat.parse(json.getString("deadline"))
                } catch (e: Exception) {
                    null
                }
            } else null
        )
    } catch (e: Exception) {
        LoggerFactory.getLogger(FileStorage::class.java).error("Ошибка парсинга TodoItem: {}", e.message)
        null
    }
}