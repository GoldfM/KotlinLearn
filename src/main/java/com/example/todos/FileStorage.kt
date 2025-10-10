package com.example.todos

import org.json.JSONArray
import java.io.File
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.slf4j.Logger

class FileStorage(private val storageFile: File) {
    private val log: Logger = LoggerFactory.getLogger(MainActivity::class.java)
    private val _items: MutableList<TodoItem> = mutableListOf()

    val items: List<TodoItem>
        get() {
            removeExpiredItems()
            return _items.toList()
        }

    fun add(item: TodoItem) {
        log.info("Добавление задачи: '{}' (приоритет: {}, дедлайн: {}, uid: {})",
            item.text, item.priority, item.deadline ?: "нет", item.uid)
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

    private fun removeExpiredItems() {
        log.debug("Проверка просроченных задач")
        val now = LocalDateTime.now()
        val expiredItems = _items.filter { i -> i.deadline != null && now.isAfter(i.deadline) }

        if (expiredItems.isNotEmpty()) {
            log.info("Найдено просроченных задач: {}", expiredItems.size)
            expiredItems.forEach { item ->
                log.info("Автоматическое удаление просроченной задачи: '{}' (uid: {})",
                    item.text, item.uid)
            }
            _items.removeAll(expiredItems.toSet())
        } else {
            log.debug("Просроченных задач не найдено")
        }
    }

    fun saveToFile() {
        log.info("Сохранение задач в файл: {}", storageFile.absolutePath)
        removeExpiredItems()
        val jsonItems = JSONArray()
        items.forEach { item -> jsonItems.put(item.json) }

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
                    val newItem = parse(jsonItem)
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
            removeExpiredItems()

        } catch (e: Exception) {
            log.error("Ошибка при загрузке из файла: {}", e.message)
        }
    }
}