package com.example.todos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.todos.ui.theme.ToDosTheme
import org.slf4j.LoggerFactory
import java.io.File


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val log = LoggerFactory.getLogger(MainActivity::class.java)
        super.onCreate(savedInstanceState)

        log.info("Приложение запущено")

        setContent {
            ToDosTheme {
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

    val repository = remember {
        val fileStorage = FileStorage(File(context.filesDir, "todo_items.json"))
        fileStorage.loadFromFile()
        TodoRepository(fileStorage)
    }

    NavHost(
        navController = navController,
        startDestination = "todo_list"
    ) {
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

        composable(
            route = "edit_todo/{todoId}",
            arguments = listOf(
                navArgument("todoId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val todoId = backStackEntry.arguments?.getString("todoId")!!

            var todoItem by remember { mutableStateOf<TodoItem?>(null) }

            LaunchedEffect(todoId) {
                todoItem = repository.getTodoById(todoId)
            }

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