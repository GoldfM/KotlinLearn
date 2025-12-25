package com.example.myapplication

import java.util.*

class TodoApiConverter {
    private val deviceId = "android-app-${UUID.randomUUID().toString().substring(0, 8)}"

    fun toNetworkTodoItem(todo: TodoItem): NetworkTodoItem {
        val now = System.currentTimeMillis() / 1000

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
            createdAt = now,
            changedAt = now,
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
            deadline = networkItem.deadline?.let { Date(it * 1000) }
        )
    }
}