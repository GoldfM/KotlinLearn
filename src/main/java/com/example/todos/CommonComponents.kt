package com.example.todos

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PriorityChip(priority: Priority) {
    val (text, color) = when (priority) {
        Priority.MINOR -> Pair("Низкий", Color.Green)
        Priority.STANDARD -> Pair("Стандартный", Color(0xFFFF9800)) // Оранжевый
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
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}