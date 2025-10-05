package com.example.todos

import androidx.compose.ui.graphics.Color
import java.util.UUID
import java.time.LocalDateTime

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
    val deadline: LocalDateTime? = null
)