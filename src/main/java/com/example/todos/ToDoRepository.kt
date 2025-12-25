package com.example.todos

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.*

class TodoRepository(
    private val fileStorage: FileStorage,
    private val syncInterval: Long = 30000 // 30 секунд
) {
    private val log = LoggerFactory.getLogger(TodoRepository::class.java)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _todosFlow = MutableStateFlow<List<TodoItem>>(emptyList())
    val todosFlow: StateFlow<List<TodoItem>> = _todosFlow.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        log.info("Инициализация репозитория")
        loadFromCache()
        startPeriodicSync()
    }

    private fun loadFromCache() {
        log.info("Загрузка задач из кэша")
        _todosFlow.value = fileStorage.items
    }

    private fun startPeriodicSync() {
        scope.launch {
            while (true) {
                delay(syncInterval)
                syncWithBackend()
            }
        }
    }

    suspend fun syncWithBackend() {
        log.info("Начало синхронизации с бэкендом")
        _syncState.value = SyncState.Syncing

        try {
            delay(1000)

            val backendItems = fetchFromBackend()

            updateCacheWithBackendData(backendItems)

            _syncState.value = SyncState.Success("Синхронизировано")
            log.info("Синхронизация успешна")

        } catch (e: Exception) {
            _syncState.value = SyncState.Error("Ошибка синхронизации: ${e.message}")
            log.error("Ошибка синхронизации: ${e.message}")
        } finally {
            delay(2000)
            _syncState.value = SyncState.Idle
        }
    }

    suspend fun addTodo(todo: TodoItem) {
        log.info("Добавление задачи: ${todo.text}")

        fileStorage.add(todo)
        fileStorage.saveToFile()

        _todosFlow.value = fileStorage.items

        scope.launch {
            sendToBackend(todo)
        }
    }

    suspend fun updateTodo(todo: TodoItem) {
        log.info("Обновление задачи: ${todo.uid}")

        fileStorage.updateItem(todo)
        fileStorage.saveToFile()
        _todosFlow.value = fileStorage.items

        scope.launch {
            updateOnBackend(todo)
        }
    }

    suspend fun deleteTodo(uid: String) {
        log.info("Удаление задачи: $uid")

        fileStorage.remove(uid)
        fileStorage.saveToFile()
        _todosFlow.value = fileStorage.items

        scope.launch {
            deleteFromBackend(uid)
        }
    }

    suspend fun getTodoById(uid: String): TodoItem? {
        return fileStorage.getItemById(uid)
    }

    private suspend fun fetchFromBackend(): List<TodoItem> {
        log.info("[Бэкенд] Запрос списка задач")
        return fileStorage.items
    }

    private suspend fun sendToBackend(todo: TodoItem) {
        log.info("[Бэкенд] Отправка задачи: ${todo.text}")
        delay(500) // Имитация сетевой задержки
        log.info("[Бэкенд] Задача отправлена успешно")
    }

    private suspend fun updateOnBackend(todo: TodoItem) {
        log.info("[Бэкенд] Обновление задачи: ${todo.uid}")
        delay(500)
        log.info("[Бэкенд] Задача обновлена успешно")
    }

    private suspend fun deleteFromBackend(uid: String) {
        log.info("[Бэкенд] Удаление задачи: $uid")
        delay(500)
        log.info("[Бэкенд] Задача удалена успешно")
    }

    private fun updateCacheWithBackendData(backendItems: List<TodoItem>) {
        log.info("Обновление кэша данными с бэкенда")
        backendItems.forEach {
            log.debug("Задача с бэкенда: ${it.text}")
        }
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}