package com.example.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTodoScreen(
    todoItem: TodoItem? = null,
    onSave: (TodoItem) -> Unit,
    onBack: () -> Unit
) {
    var editState by remember {
        mutableStateOf(
            EditTodoState(
                text = todoItem?.text ?: "",
                priority = todoItem?.priority ?: Priority.STANDARD,
                isDone = todoItem?.isDone ?: false,
                deadline = todoItem?.deadline
            )
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(editState.text)) }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (todoItem == null) "Новое дело" else "Редактирование") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Назад")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        val newItem = TodoItem(
                            text = editState.text,
                            priority = editState.priority,
                            isDone = editState.isDone,
                            deadline = editState.deadline
                        )
                        onSave(newItem)
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = editState.text.isNotBlank()
                ) {
                    Text("✔ Сохранить")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .alpha(if (editState.isDone) 0.6f else 1f)
        ) {
            // Поле ввода текста
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    editState = editState.copy(text = it.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Название дела") },
                placeholder = { Text("Введите текст дела...") },
                singleLine = false,
                maxLines = 10,
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )

            // Чекбокс "Выполнено"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = editState.isDone,
                    onCheckedChange = { checked ->
                        editState = editState.copy(isDone = checked)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выполнено")
            }

            // Выбор приоритета
            PrioritySelector(
                selectedPriority = editState.priority,
                onPrioritySelected = { priority ->
                    editState = editState.copy(priority = priority)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Выбор дедлайна
            DeadlineSelector(
                deadline = editState.deadline,
                onDeadlineSelected = { date ->
                    editState = editState.copy(deadline = date)
                },
                onClearDeadline = {
                    editState = editState.copy(deadline = null)
                },
                showDatePicker = { showDatePicker = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Простой диалог выбора даты
    if (showDatePicker) {
        SimpleDatePickerDialog(
            onDateSelected = { date ->
                editState = editState.copy(deadline = date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun PrioritySelector(
    selectedPriority: Priority,
    onPrioritySelected: (Priority) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Приоритет")
                PriorityChip(priority = selectedPriority)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Priority.values().forEach { priority ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(getPriorityText(priority))
                            Spacer(modifier = Modifier.width(8.dp))
                            PriorityChip(priority = priority)
                        }
                    },
                    onClick = {
                        onPrioritySelected(priority)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PriorityChip(priority: Priority) {
    val (text, color) = when (priority) {
        Priority.MINOR -> Pair("Низкий", Color.Green)
        Priority.STANDARD -> Pair("Стандартный", Color.Yellow)
        Priority.CRITICAL -> Pair("Высокий", Color.Red)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "○",
                color = color,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun DeadlineSelector(
    deadline: Date?,
    onDeadlineSelected: (Date) -> Unit,
    onClearDeadline: () -> Unit,
    showDatePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Дедлайн")

            if (deadline != null) {
                TextButton(onClick = onClearDeadline) {
                    Text("Очистить")
                }
            }
        }

        Surface(
            onClick = showDatePicker,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = deadline?.let { dateFormat.format(it) } ?: "Не установлен",
                    color = if (deadline == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "📅",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SimpleDatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(calendar.time) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите дату") },
        text = {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            Column {
                // Простой выбор даты - добавление дней
                Text("Выбрано: ${dateFormat.format(selectedDate)}")

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        calendar.time = selectedDate
                        calendar.add(Calendar.DAY_OF_MONTH, -1)
                        selectedDate = calendar.time
                    }) {
                        Text("-1 день")
                    }

                    Button(onClick = {
                        calendar.time = Date() // Сегодня
                        selectedDate = calendar.time
                    }) {
                        Text("Сегодня")
                    }

                    Button(onClick = {
                        calendar.time = selectedDate
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                        selectedDate = calendar.time
                    }) {
                        Text("+1 день")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Быстрый выбор
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        calendar.time = Date()
                        calendar.add(Calendar.DAY_OF_MONTH, 7)
                        selectedDate = calendar.time
                    }) {
                        Text("Через неделю")
                    }

                    Button(onClick = {
                        calendar.time = Date()
                        calendar.add(Calendar.MONTH, 1)
                        selectedDate = calendar.time
                    }) {
                        Text("Через месяц")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(selectedDate)
                }
            ) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun getPriorityText(priority: Priority): String {
    return when (priority) {
        Priority.MINOR -> "Низкий приоритет"
        Priority.STANDARD -> "Стандартный приоритет"
        Priority.CRITICAL -> "Высокий приоритет"
    }
}