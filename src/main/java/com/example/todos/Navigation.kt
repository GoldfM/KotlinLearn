package com.example.todos

sealed class Screen(val route: String) {
    object TodoList : Screen("todo_list")
    object EditTodo : Screen("edit_todo") {
        const val ARG_TODO_ID = "todo_id"
        fun createRoute(todoId: String? = null) =
            if (todoId != null) "edit_todo/$todoId" else "edit_todo"
    }
}