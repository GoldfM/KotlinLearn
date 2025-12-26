package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodoApp()
                }
            }
        }
    }
}

@Composable
fun TodoApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Создаем репозиторий с Room вместо FileStorage
    val repository = remember {
        TodoRepository(context)
    }

    NavHost(
        navController = navController,
        startDestination = "todo_list"
    ) {
        // Экран списка задач
        composable("todo_list") {
            TodoListScreen(
                repository = repository,
                onTodoClick = { todo ->
                    navController.navigate("edit_todo/${todo.uid}")
                },
                onAddTodo = {
                    navController.navigate("edit_todo")
                }
            )
        }

        // Экран создания новой задачи
        composable("edit_todo") {
            EditTodoScreen(
                todoItem = null,
                repository = repository,
                onSave = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран редактирования существующей задачи
        composable(
            route = "edit_todo/{todoId}",
            arguments = listOf(
                navArgument("todoId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val todoId = backStackEntry.arguments?.getString("todoId")!!

            // Состояние для хранения загруженной задачи
            var todoItem by remember { mutableStateOf<TodoItem?>(null) }

            // Загружаем задачу по ID при открытии экрана
            LaunchedEffect(todoId) {
                todoItem = repository.getTodoById(todoId)
            }

            // Показываем экран редактирования или индикатор загрузки
            if (todoItem != null) {
                EditTodoScreen(
                    todoItem = todoItem,
                    repository = repository,
                    onSave = {
                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                // Индикатор загрузки пока задача загружается
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}