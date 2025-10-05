package com.example.todos
import com.example.todos.ui.theme.ToDosTheme
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import java.io.File
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fileStorage = FileStorage(File(this.filesDir, "todo_items.json"))

        val item1 = TodoItem(text = "Проснуться", color = Color.Red,
            priority = Priority.CRITICAL, deadline = LocalDateTime.now().plusSeconds(5))
        fileStorage.add(item1)
        val item2 = TodoItem(text = "Потянуться", priority = Priority.STANDARD)
        fileStorage.add(item2)
        val item3 = TodoItem(text = "Поспать", priority = Priority.MINOR, color = Color.Yellow,
            deadline = LocalDateTime.now().plusMinutes(5))
        fileStorage.add(item3)


        Log.d("User logs","После добавления: ${fileStorage.items}")

        fileStorage.saveToFile()
        Thread.sleep(6000)

        Log.d("User logs","После дедлайна: ${fileStorage.items}")

        fileStorage.remove(item3.uid).let {
            Log.d("User logs","После удаления: ${fileStorage.items}")
        }

        fileStorage.loadFromFile()
        Log.d("User logs","После загрузки: ${fileStorage.items}")

        setContent {
            ToDosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("World")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ToDosTheme {
        Greeting("World")
    }
}