package com.example.myapplication

import androidx.compose.ui.graphics.Color
import java.util.Date
import java.util.UUID

enum class Priority {
    MINOR,
    STANDARD,
    CRITICAL
}

data class TodoItem(
    val text: String,
    val priority: Priority,
    val uid: String = UUID.randomUUID().toString(),
    val isDone: Boolean = false,
    val color: Color = Color.White,
    val deadline: Date? = null,
    // Добавляем временные метки для синхронизации
    val createdAt: Long = System.currentTimeMillis(),
    val changedAt: Long = System.currentTimeMillis()
)

data class EditTodoState(
    val text: String = "",
    val priority: Priority = Priority.STANDARD,
    val isDone: Boolean = false,
    val deadline: Date? = null
)

// Конвертация TodoItem <-> TodoEntity
fun TodoItem.toEntity(): TodoEntity {
    return TodoEntity(
        uid = uid,
        text = text,
        priority = priority.name,
        isDone = isDone,
        color = color.value.toLong(),
        deadline = deadline?.time,
        createdAt = createdAt, // Сохраняем оригинальное время создания
        updatedAt = System.currentTimeMillis(), // Время последнего обновления
        isSynced = false
    )
}

fun TodoEntity.toTodoItem(): TodoItem {
    return TodoItem(
        uid = uid,
        text = text,
        priority = Priority.valueOf(priority),
        isDone = isDone,
        color = Color(color.toULong()),
        deadline = deadline?.let { Date(it) },
        createdAt = createdAt, // Восстанавливаем время создания
        changedAt = updatedAt   // Используем updatedAt как changedAt
    )
}