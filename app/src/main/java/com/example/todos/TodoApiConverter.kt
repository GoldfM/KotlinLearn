package com.example.myapplication

import java.util.*

class TodoApiConverter {
    private val deviceId = "android-app-${UUID.randomUUID().toString().substring(0, 8)}"

    fun toNetworkTodoItem(todo: TodoItem, originalCreatedAt: Long? = null): NetworkTodoItem {
        // ВАЖНО: Для обновления существующих задач нужно использовать
        // ОРИГИНАЛЬНЫЙ created_at, а не текущее время

        // Если передан originalCreatedAt (при обновлении), используем его
        // Иначе (при создании новой задачи) используем текущее время
        val createdAt = originalCreatedAt ?: (System.currentTimeMillis() / 1000)

        // changedAt всегда должен быть больше предыдущего значения
        val changedAt = System.currentTimeMillis() / 1000

        return NetworkTodoItem(
            id = todo.uid,
            text = todo.text,
            importance = when (todo.priority) {
                Priority.MINOR -> "low"
                Priority.STANDARD -> "basic"
                Priority.CRITICAL -> "important"
            },
            deadline = todo.deadline?.time?.div(1000),
            done = todo.isDone,
            color = "#FFFFFF",
            // КРИТИЧЕСКИ ВАЖНО: createdAt должен быть оригинальным
            createdAt = createdAt,
            // changedAt всегда должен быть больше предыдущего
            changedAt = changedAt,
            lastUpdatedBy = deviceId
        )
    }

    fun toLocalTodoItem(networkItem: NetworkTodoItem): TodoItem {
        return TodoItem(
            uid = networkItem.id,
            text = networkItem.text,
            priority = when (networkItem.importance) {
                "low" -> Priority.MINOR
                "basic" -> Priority.STANDARD
                "important" -> Priority.CRITICAL
                else -> Priority.STANDARD
            },
            isDone = networkItem.done,
            color = androidx.compose.ui.graphics.Color.White,
            deadline = networkItem.deadline?.let { Date(it * 1000) },
            // Сохраняем оригинальные временные метки с сервера
            createdAt = networkItem.createdAt * 1000, // конвертируем секунды в мс
            changedAt = networkItem.changedAt * 1000
        )
    }
}