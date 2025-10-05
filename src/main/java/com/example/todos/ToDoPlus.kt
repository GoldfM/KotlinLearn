package com.example.todos

import org.json.JSONObject
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val TodoItem.json: JSONObject
    get() = JSONObject().apply {
        put("uid", uid)
        put("text", text)
        put("isDone", isDone)
        if(priority != Priority.STANDARD)
            put("priority", priority)

        if(color != Color.White)
            put("color", color.toArgb())

        if(deadline != null)
            put("deadline", deadline.toString())
    }

fun parse(json: JSONObject): TodoItem? {
    try {
        val uid = json.getString("uid")
        val priority = when (json.optString("priority")) {
            "CRITICAL" -> Priority.CRITICAL
            "STANDARD" -> Priority.STANDARD
            else -> Priority.MINOR
        }
        val isDone = json.getBoolean("isDone")
        val text = json.getString("text")
        val color = if (json.has("color")) {
            Color(json.getInt("color"))
        } else {
            Color.Green
        }

        val deadline = if (json.has("deadline")) {
            LocalDateTime.parse(json.getString("deadline"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } else {
            null
        }
        return TodoItem(priority=priority, uid = uid, text = text,
            deadline = deadline, isDone = isDone, color = color)
    }
    catch (e: Exception){
        return null
    }
}