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
    val deadline: Date? = null
)

data class EditTodoState(
    val text: String = "",
    val priority: Priority = Priority.STANDARD,
    val isDone: Boolean = false,
    val deadline: Date? = null
)

// Добавь эти расширения в конец файла:
fun TodoItem.toEntity(): TodoEntity {
    return TodoEntity(
        uid = uid,
        text = text,
        priority = priority.name,
        isDone = isDone,
        color = color.value.toLong(),
        deadline = deadline?.time,
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
        deadline = deadline?.let { Date(it) }
    )
}