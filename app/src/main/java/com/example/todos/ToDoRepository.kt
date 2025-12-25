package com.example.myapplication

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import android.content.Context

class TodoRepository(
    context: Context
) {
    private val log = LoggerFactory.getLogger(TodoRepository::class.java)
    private val apiClient = TodoApiClient()
    private val converter = TodoApiConverter()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val database = TodoDatabase.getInstance(context)
    private val dao = database.todoDao()

    private val _todosFlow = MutableStateFlow<List<TodoItem>>(emptyList())
    val todosFlow: StateFlow<List<TodoItem>> = _todosFlow.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        loadFromCache()
        scope.launch {
            syncWithServer()
        }
    }

    private fun loadFromCache() {
        scope.launch {
            dao.getAll().collect { entities ->
                _todosFlow.value = entities.map { it.toTodoItem() }
            }
        }
    }

    suspend fun syncWithServer() {
        _syncState.value = SyncState.Syncing

        try {
            // 1. Загружаем с сервера
            val networkItems = apiClient.loadTodos()
            val serverItems = networkItems.map { converter.toLocalTodoItem(it) }

            // 2. Получаем локальные изменения
            val unsyncedItems = dao.getUnsynced()

            // 3. Синхронизируем: сначала отправляем локальные изменения
            unsyncedItems.forEach { entity ->
                val todoItem = entity.toTodoItem()
                val networkItem = converter.toNetworkTodoItem(todoItem)
                if (apiClient.addTodo(networkItem)) {
                    dao.markAsSynced(entity.uid)
                }
            }

            // 4. Обновляем локальную БД данными с сервера
            serverItems.forEach { todoItem ->
                dao.insert(todoItem.toEntity().copy(isSynced = true))
            }

            _syncState.value = SyncState.Success("Синхронизировано")

        } catch (e: Exception) {
            _syncState.value = SyncState.Error("Ошибка: ${e.message}")
        }
    }

    suspend fun addTodo(todo: TodoItem) {
        scope.launch {
            val entity = todo.toEntity().copy(isSynced = false)
            dao.insert(entity)

            // Фоновая синхронизация
            syncWithServer()
        }
    }

    suspend fun updateTodo(todo: TodoItem) {
        scope.launch {
            val entity = todo.toEntity().copy(isSynced = false, updatedAt = System.currentTimeMillis())
            dao.update(entity)

            // Фоновая синхронизация
            syncWithServer()
        }
    }

    suspend fun deleteTodo(uid: String) {
        scope.launch {
            // Сначала удаляем локально
            dao.deleteById(uid)

            // Затем пытаемся удалить на сервере
            try {
                apiClient.deleteTodo(uid)
            } catch (e: Exception) {
                log.error("Ошибка при удалении с сервера: ${e.message}")
            }
        }
    }

    suspend fun getTodoById(uid: String): TodoItem? {
        return dao.getById(uid)?.toTodoItem()
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}