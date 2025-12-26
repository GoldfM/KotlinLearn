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

    val todosFlow: Flow<List<TodoItem>> = dao.getAll().map { entities ->
        entities.map { it.toTodoItem() }
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        loadFromCache()
        scope.launch {
            delay(2000) // Небольшая задержка для старта приложения
            syncWithServer()
        }
    }

    private fun loadFromCache() {
        scope.launch {
            val todos = withContext(Dispatchers.IO) {
                dao.getAll().first() // Получаем первое значение Flow
            }
            log.info("📋 Загружено ${todos.size} задач из локальной БД")
        }
    }

    suspend fun syncWithServer() {
        log.info("🔄 НАЧАЛО СИНХРОНИЗАЦИИ")
        _syncState.value = SyncState.Syncing

        try {
            // 1. СНАЧАЛА отправляем все локальные изменения на сервер
            val unsyncedItems = dao.getUnsynced()
            log.info("📤 Найдено ${unsyncedItems.size} несинхронизированных задач")

            if (unsyncedItems.isNotEmpty()) {
                log.info("Отправка локальных изменений на сервер...")

                unsyncedItems.forEach { entity ->
                    val todoItem = entity.toTodoItem()

                    // ПЕРЕДАЕМ оригинальный created_at из entity
                    val networkItem = converter.toNetworkTodoItem(
                        todo = todoItem,
                        originalCreatedAt = entity.createdAt / 1000 // конвертируем мс в секунды
                    )

                    val updateSuccess = apiClient.updateTodo(networkItem)

                    if (updateSuccess) {
                        log.info("   ✅ Задача обновлена на сервере")
                        dao.markAsSynced(entity.uid)
                    } else {
                        log.warn("   ⚠️ Update не сработал, пробуем добавить как новую")
                        // Для add тоже нужен оригинальный created_at
                        val addSuccess = apiClient.addTodo(networkItem)
                        if (addSuccess) {
                            log.info("   ✅ Задача добавлена на сервер")
                            dao.markAsSynced(entity.uid)
                        } else {
                            log.error("   ❌ Не удалось отправить задачу на сервер")
                        }
                    }
                }
            } else {
                log.info("📭 Нет несинхронизированных задач для отправки")
            }

            // 2. ПОТОМ загружаем актуальные данные с сервера
            log.info("📥 Загрузка данных с сервера...")
            val networkItems = apiClient.loadTodos()
            val serverItems = networkItems.map { converter.toLocalTodoItem(it) }

            log.info("📊 Получено ${serverItems.size} задач с сервера")

            // 3. Обновляем локальную БД, но не трогаем недавно отправленные задачи
            serverItems.forEach { serverTodo ->
                val localEntity = dao.getById(serverTodo.uid)

                when {
                    // Задачи нет локально - добавляем
                    localEntity == null -> {
                        log.info("   + Добавлена новая задача с сервера: ${serverTodo.text}")
                        dao.insert(serverTodo.toEntity().copy(isSynced = true))
                    }
                    // Задача уже синхронизирована - обновляем с сервера
                    localEntity.isSynced -> {
                        // Проверяем, действительно ли данные отличаются
                        if (localEntity.text != serverTodo.text ||
                            localEntity.isDone != serverTodo.isDone) {
                            log.info("   ↻ Обновление синхронизированной задачи: ${serverTodo.text}")
                            dao.insert(serverTodo.toEntity().copy(isSynced = true))
                        }
                    }
                    // Задача несинхронизирована - НЕ трогаем, ждем пока отправится
                    else -> {
                        log.info("   ⏸️ Пропуск несинхронизированной задачи: ${serverTodo.text}")
                    }
                }
            }

            log.info("✅ СИНХРОНИЗАЦИЯ ЗАВЕРШЕНА")
            _syncState.value = SyncState.Success("Синхронизировано")

        } catch (e: Exception) {
            log.error("❌ ОШИБКА СИНХРОНИЗАЦИИ: ${e.message}")
            e.printStackTrace()
            _syncState.value = SyncState.Error("Ошибка: ${e.message}")
        }
    }

    suspend fun addTodo(todo: TodoItem) {
        log.info("➕ ДОБАВЛЕНИЕ новой задачи: ${todo.text}")
        scope.launch {
            val entity = todo.toEntity().copy(isSynced = false)
            dao.insert(entity)
            log.info("✅ Задача сохранена в локальную БД")

            // Фоновая синхронизация
            syncWithServer()
        }
    }

    suspend fun updateTodo(todo: TodoItem) {
        log.info("✏️ ОБНОВЛЕНИЕ задачи: ${todo.text} (ID: ${todo.uid})")
        scope.launch {
            val entity = todo.toEntity().copy(
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
            dao.insert(entity)
            log.info("✅ Изменения сохранены в локальную БД")

            // Фоновая синхронизация
            syncWithServer()
        }
    }

    suspend fun deleteTodo(uid: String) {
        log.info("🗑️ УДАЛЕНИЕ задачи с ID: $uid")
        scope.launch {
            val entity = dao.getById(uid)
            if (entity != null) {
                dao.delete(entity)
                log.info("✅ Задача удалена из локальной БД")

                // Пытаемся удалить с сервера
                try {
                    if (apiClient.deleteTodo(uid)) {
                        log.info("✅ Задача удалена с сервера")
                    } else {
                        log.warn("⚠️ Не удалось удалить задачу с сервера")
                    }
                } catch (e: Exception) {
                    log.error("❌ Ошибка при удалении с сервера: ${e.message}")
                }
            } else {
                log.warn("⚠️ Задача с ID $uid не найдена в локальной БД")
            }
        }
    }

    suspend fun getTodoById(uid: String): TodoItem? {
        return withContext(Dispatchers.IO) {
            dao.getById(uid)?.toTodoItem()
        }
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}