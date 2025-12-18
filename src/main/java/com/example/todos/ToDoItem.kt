package com.example.todos

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