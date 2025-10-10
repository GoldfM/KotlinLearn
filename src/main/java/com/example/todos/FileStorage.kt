package com.example.todos

import java.io.File
import java.time.LocalDateTime
import org.json.JSONArray

class FileStorage(private val storageFile: File) {

    private val _items: MutableList<TodoItem> = mutableListOf()

    val items: List<TodoItem>
        get() {
            removeExpiredItems()
            return _items.toList()
        }
    fun loadFromFile() {
        if (storageFile.exists().not()) return

        val jsonContent = storageFile.readText()
        val jsonArray = JSONArray(jsonContent)
        _items.clear()

        for (index in 0 until jsonArray.length()) {
            parse(jsonArray.getJSONObject(index))?.let { _items.add(it) }
        }
        removeExpiredItems()
    }

    fun saveToFile() {
        removeExpiredItems()
        val jsonArray = JSONArray()
        items.forEach { jsonArray.put(it.json) }
        storageFile.writeText(jsonArray.toString())
    }
    fun add(item: TodoItem) {
        _items.add(item)
    }

    fun remove(uid: String): Boolean = _items.removeIf { it.uid == uid }



    private fun removeExpiredItems() {
        val currentTime = LocalDateTime.now()
        val expired = _items.filter { it.deadline?.let { time -> currentTime.isAfter(time) } ?: false }
        expired.forEach { remove(it.uid) }
    }


}