package com.example.todos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    val fileStorage = remember {
        FileStorage(File(context.filesDir, "todo_items.json"))
    }
    fileStorage.loadFromFile()

    NavHost(
        navController = navController,
        startDestination = "todo_list" // Стартовая точка
    ) {
        // 1. Экран списка
        composable("todo_list") {
            TodoListScreen(
                fileStorage = fileStorage,
                onTodoClick = { todo ->
                    // Переход к редактированию существующей задачи с её ID
                    navController.navigate("edit_todo/${todo.uid}")
                },
                onAddTodo = {
                    // Переход к созданию новой задачи (без ID)
                    navController.navigate("edit_todo")
                }
            )
        }

        // 2. Экран создания новой задачи (БЕЗ параметра)
        composable("edit_todo") { backStackEntry ->
            // todoItem = null, поэтому создаем новую задачу
            EditTodoScreen(
                todoItem = null,
                onSave = { savedTodo ->
                    fileStorage.add(savedTodo)
                    fileStorage.saveToFile()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 3. Экран редактирования существующей задачи (С параметром)
        composable(
            route = "edit_todo/{todoId}",
            arguments = listOf(
                navArgument("todoId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val todoId = backStackEntry.arguments?.getString("todoId")!!
            val todoItem = fileStorage.getItemById(todoId)

            EditTodoScreen(
                todoItem = todoItem,
                onSave = { savedTodo ->
                    fileStorage.updateItem(savedTodo)
                    fileStorage.saveToFile()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}