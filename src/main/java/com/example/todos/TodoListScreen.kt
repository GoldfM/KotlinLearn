package com.example.todos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    fileStorage: FileStorage,
    onTodoClick: (TodoItem) -> Unit,
    onAddTodo: () -> Unit
) {
    val todos = remember { mutableStateListOf<TodoItem>() }
    var deletedItem by remember { mutableStateOf<TodoItem?>(null) }
    var showUndoSnackbar by remember { mutableStateOf(false) }

    // Загружаем задачи из хранилища
    LaunchedEffect(Unit) {
        todos.clear()
        todos.addAll(fileStorage.items)
    }

    // Функция для обновления задачи в списке
    val updateTodoInList = { updatedTodo: TodoItem ->
        val index = todos.indexOfFirst { it.uid == updatedTodo.uid }
        if (index != -1) {
            todos[index] = updatedTodo
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Список дел") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTodo,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Добавить задачу",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (todos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Нет задач",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Нажмите '+' чтобы добавить первую задачу",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(
                        items = todos,
                        key = { it.uid },
                        itemContent = { todo ->
                            SwipeToDeleteTodoItem(
                                todo = todo,
                                fileStorage = fileStorage,
                                onTodoClick = { onTodoClick(todo) },
                                onDelete = {
                                    deletedItem = todo
                                    showUndoSnackbar = true
                                    val index = todos.indexOf(todo)
                                    if (index != -1) {
                                        todos.removeAt(index)
                                        fileStorage.remove(todo.uid)
                                        fileStorage.saveToFile()
                                    }
                                },
                                onToggleDone = { updatedTodo ->
                                    updateTodoInList(updatedTodo)
                                    // fileStorage.updateItem уже вызывается внутри SwipeToDeleteTodoItem
                                }
                            )
                        }
                    )
                }
            }

            // Уведомление об удалении с возможностью отмены
            AnimatedVisibility(
                visible = showUndoSnackbar,
                enter = expandVertically(expandFrom = Alignment.Bottom),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Задача удалена",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(
                                onClick = {
                                    deletedItem?.let { item ->
                                        todos.add(item)
                                        fileStorage.add(item)
                                        fileStorage.saveToFile()
                                    }
                                    showUndoSnackbar = false
                                    deletedItem = null
                                }
                            ) {
                                Text("Отменить")
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteTodoItem(
    todo: TodoItem,
    fileStorage: FileStorage,
    onTodoClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleDone: (TodoItem) -> Unit
) {
    // Переменная для отслеживания, нужно ли удалить элемент
    var shouldDelete by remember { mutableStateOf(false) }

    // Используем состояние для SwipeToDismissBox
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> { // Свайп влево = удаление
                    shouldDelete = true // Помечаем для удаления
                    true // Возвращаем true для анимации удаления
                }
                SwipeToDismissBoxValue.StartToEnd -> { // Свайп вправо = отметка выполненным
                    val updatedTodo = todo.copy(isDone = !todo.isDone)
                    onToggleDone(updatedTodo) // Обновляем состояние
                    fileStorage.updateItem(updatedTodo) // Сохраняем в хранилище
                    fileStorage.saveToFile()
                    false // Возвращаем false, чтобы элемент остался на месте
                }
                else -> false
            }
        }
    )

    // Если нужно удалить - вызываем onDelete
    if (shouldDelete) {
        LaunchedEffect(Unit) {
            // Даем время для завершения анимации свайпа
            kotlinx.coroutines.delay(300)
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        modifier = Modifier.fillMaxWidth(),
        // Фоновый контент, который показывается при свайпе
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = when (swipeToDismissBoxState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.Center
                }
            ) {
                when (swipeToDismissBoxState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Red),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Удалить",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Удалить",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (todo.isDone) Color.Gray else Color.Green),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                Icon(
                                    if (todo.isDone) Icons.Filled.Close else Icons.Filled.Check,
                                    contentDescription = if (todo.isDone) "Не выполнено" else "Выполнено",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (todo.isDone) "Не выполнено" else "Выполнено",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    ) {
        // Основной контент — карточка задачи
        TodoItemCard(
            todo = todo,
            onClick = onTodoClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
@Composable
fun TodoItemCard(
    todo: TodoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .alpha(if (todo.isDone) 0.6f else 1f),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = todo.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    todo.deadline?.let { deadline ->
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        Text(
                            text = "До: ${dateFormat.format(deadline)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                PriorityChip(priority = todo.priority)
            }

            // Статус выполнения
            Text(
                text = if (todo.isDone) "✓ Выполнено" else "⏳ В процессе",
                style = MaterialTheme.typography.labelSmall,
                color = if (todo.isDone) Color.Green else Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}