package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

@Entity(tableName = "todo_items")
data class TodoEntity(
    @PrimaryKey
    val uid: String = UUID.randomUUID().toString(),
    val text: String,
    val priority: String, // "MINOR", "STANDARD", "CRITICAL"
    val isDone: Boolean = false,
    val color: Long = -1L, // Храним как Long
    val deadline: Long? = null, // Храним timestamp (Date.time)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)