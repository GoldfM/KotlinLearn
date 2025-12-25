package com.example.todos

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    repository: TodoRepository,
    onTodoClick: (TodoItem) -> Unit,
    onAddTodo: () -> Unit
) {
    val todos by repository.todosFlow.collectAsState()
    val syncState by repository.syncState.collectAsState()

    var deletedItem by remember { mutableStateOf<TodoItem?>(null) }
    var showUndoSnackbar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        repository.syncWithBackend()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Список дел")
                        Spacer(modifier = Modifier.width(8.dp))

                        when (syncState) {
                            is SyncState.Syncing -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            is SyncState.Success -> {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Синхронизировано",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Green
                                )
                            }
                            is SyncState.Error -> {
                                Icon(
                                    Icons.Filled.Error,
                                    contentDescription = "Ошибка синхронизации",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Red
                                )
                            }
                            else -> {}
                        }
                    }
                },
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

                    Button(
                        onClick = {
                            scope.launch {
                                repository.syncWithBackend()
                            }
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Синхронизировать с бэкендом")
                    }
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
                                repository = repository,
                                onTodoClick = { onTodoClick(todo) },
                                onDelete = {
                                    deletedItem = todo
                                    showUndoSnackbar = true
                                    scope.launch {
                                        repository.deleteTodo(todo.uid)
                                    }
                                },
                                onToggleDone = { updatedTodo ->
                                    scope.launch {
                                        repository.updateTodo(updatedTodo)
                                    }
                                }
                            )
                        }
                    )
                }
            }

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
                                        scope.launch {
                                            repository.addTodo(item)
                                        }
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
    repository: TodoRepository,
    onTodoClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleDone: (TodoItem) -> Unit
) {
    var shouldDelete by remember { mutableStateOf(false) }

    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    shouldDelete = true
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    val updatedTodo = todo.copy(isDone = !todo.isDone)
                    onToggleDone(updatedTodo)
                    true
                }
                else -> false
            }
        }
    )

    if (shouldDelete) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(300)
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        modifier = Modifier.fillMaxWidth(),
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

            Text(
                text = if (todo.isDone) "✓ Выполнено" else "⏳ В процессе",
                style = MaterialTheme.typography.labelSmall,
                color = if (todo.isDone) Color.Green else Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}