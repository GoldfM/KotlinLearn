package com.example.todos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todos.ui.theme.ToDosTheme
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val log = LoggerFactory.getLogger(MainActivity::class.java)
        super.onCreate(savedInstanceState)

        log.info("Приложение запущено")

        val fileStorage = FileStorage(File(this.filesDir, "todo_items.json"))

        setContent {
            ToDosTheme {
                TodoApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp() {
    var showEditScreen by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    val todos = remember { mutableStateListOf<TodoItem>() }

    // Добавим несколько тестовых задач
    if (todos.isEmpty()) {
        val calendar = Calendar.getInstance()

        // Задача 1 - на завтра
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        todos.add(
            TodoItem(
                text = "Купить продукты",
                priority = Priority.STANDARD,
                deadline = calendar.time
            )
        )

        // Задача 2 - на послезавтра
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, 2)
        todos.add(
            TodoItem(
                text = "Подготовить отчет",
                priority = Priority.CRITICAL,
                deadline = calendar.time
            )
        )

        // Задача 3 - без дедлайна
        todos.add(
            TodoItem(
                text = "Позвонить другу",
                priority = Priority.MINOR
            )
        )
    }

    if (showEditScreen) {
        EditTodoScreen(
            todoItem = editingTodo,
            onSave = { todo ->
                if (editingTodo != null) {
                    val index = todos.indexOfFirst { it.uid == editingTodo!!.uid }
                    if (index != -1) {
                        todos[index] = todo
                    }
                } else {
                    todos.add(todo)
                }
                showEditScreen = false
                editingTodo = null
            },
            onBack = {
                showEditScreen = false
                editingTodo = null
            }
        )
    } else {
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
                    onClick = {
                        editingTodo = null
                        showEditScreen = true
                    }
                ) {
                    Text("+")
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(todos, key = { it.uid }) { todo ->
                    TodoItemCard(
                        todo = todo,
                        onEdit = {
                            editingTodo = todo
                            showEditScreen = true
                        },
                        onToggleDone = {
                            val index = todos.indexOf(todo)
                            if (index != -1) {
                                todos[index] = todo.copy(isDone = !todo.isDone)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TodoItemCard(
    todo: TodoItem,
    onEdit: () -> Unit,
    onToggleDone: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .alpha(if (todo.isDone) 0.6f else 1f),
        onClick = onEdit
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(
                    text = todo.text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                PriorityChip(priority = todo.priority)
            }

            todo.deadline?.let { deadline ->
                val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                Text(
                    text = "До: ${dateFormat.format(deadline)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(
                onClick = onToggleDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Text(if (todo.isDone) "Отметить невыполненным" else "Отметить выполненным")
            }
        }
    }
}