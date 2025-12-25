package com.example.myapplication

import androidx.room.TypeConverter
import androidx.compose.ui.graphics.Color
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun colorToLong(color: Color): Long = color.value.toLong()

    @TypeConverter
    fun longToColor(value: Long): Color = Color(value.toULong())

    @TypeConverter
    fun priorityToString(priority: Priority): String = priority.name

    @TypeConverter
    fun stringToPriority(value: String): Priority = Priority.valueOf(value)
}