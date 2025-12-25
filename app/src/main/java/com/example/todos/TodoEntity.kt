package com.example.myapplication

import androidx.room.*
import androidx.compose.ui.graphics.Color
import java.util.*

@Entity(tableName = "todo_items")
data class TodoEntity(
    @PrimaryKey
    val uid: String = UUID.randomUUID().toString(),
    val text: String,
    val priority: String,
    val isDone: Boolean = false,
    val color: Long = Color.White.value.toLong(),
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)