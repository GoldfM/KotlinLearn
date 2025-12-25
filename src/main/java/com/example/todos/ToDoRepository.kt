package com.example.todos

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory

class TodoRepository(
    private val fileStorage: FileStorage
) {
    private val log = LoggerFactory.getLogger(TodoRepository::class.java)
    private val apiClient = TodoApiClient()
    private val converter = TodoApiConverter()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _todosFlow = MutableStateFlow<List<TodoItem>>(emptyList())
    val todosFlow: StateFlow<List<TodoItem>> = _todosFlow.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        loadFromCache()
        scope.launch {
            loadFromServer()
        }
    }

    private fun loadFromCache() {
        _todosFlow.value = fileStorage.items
    }

    suspend fun loadFromServer() {
        _syncState.value = SyncState.Syncing

        try {
            val networkItems = apiClient.loadTodos()
            val localItems = networkItems.map { converter.toLocalTodoItem(it) }

            fileStorage.items.forEach { fileStorage.remove(it.uid) }
            localItems.forEach { fileStorage.add(it) }
            fileStorage.saveToFile()

            _todosFlow.value = localItems
            _syncState.value = SyncState.Success("Загружено ${localItems.size} задач")
        } catch (e: Exception) {
            _syncState.value = SyncState.Error("Ошибка: ${e.message}")
        }
    }

    suspend fun addTodo(todo: TodoItem) {
        _syncState.value = SyncState.Syncing

        try {
            loadFromServer()
            val networkTodo = converter.toNetworkTodoItem(todo)
            val success = apiClient.addTodo(networkTodo)

            if (success) {
                loadFromServer()
                _syncState.value = SyncState.Success("Задача добавлена")
            } else {
                fileStorage.add(todo)
                fileStorage.saveToFile()
                _todosFlow.value = fileStorage.items
                _syncState.value = SyncState.Error("Задача сохранена локально")
            }
        } catch (e: Exception) {
            fileStorage.add(todo)
            fileStorage.saveToFile()
            _todosFlow.value = fileStorage.items
            _syncState.value = SyncState.Error("Ошибка: ${e.message}")
        }
    }

    suspend fun updateTodo(todo: TodoItem) {
        _syncState.value = SyncState.Syncing

        try {
            loadFromServer()
            val networkTodo = converter.toNetworkTodoItem(todo)
            val success = apiClient.updateTodo(networkTodo)

            if (success) {
                loadFromServer()
                _syncState.value = SyncState.Success("Задача обновлена")
            } else {
                fileStorage.updateItem(todo)
                fileStorage.saveToFile()
                _todosFlow.value = fileStorage.items
                _syncState.value = SyncState.Error("Задача обновлена локально")
            }
        } catch (e: Exception) {
            fileStorage.updateItem(todo)
            fileStorage.saveToFile()
            _todosFlow.value = fileStorage.items
            _syncState.value = SyncState.Error("Ошибка: ${e.message}")
        }
    }

    suspend fun deleteTodo(uid: String) {
        _syncState.value = SyncState.Syncing

        try {
            loadFromServer()
            val success = apiClient.deleteTodo(uid)

            if (success) {
                loadFromServer()
                _syncState.value = SyncState.Success("Задача удалена")
            } else {
                fileStorage.remove(uid)
                fileStorage.saveToFile()
                _todosFlow.value = fileStorage.items
                _syncState.value = SyncState.Error("Задача удалена локально")
            }
        } catch (e: Exception) {
            fileStorage.remove(uid)
            fileStorage.saveToFile()
            _todosFlow.value = fileStorage.items
            _syncState.value = SyncState.Error("Ошибка: ${e.message}")
        }
    }

    suspend fun getTodoById(uid: String): TodoItem? {
        return fileStorage.getItemById(uid)
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}