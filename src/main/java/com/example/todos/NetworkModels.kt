package com.example.todos

data class NetworkTodoItem(
    val id: String,
    val text: String,
    val importance: String,
    val deadline: Long?,
    val done: Boolean,
    val color: String?,
    val createdAt: Long,
    val changedAt: Long,
    val lastUpdatedBy: String
)