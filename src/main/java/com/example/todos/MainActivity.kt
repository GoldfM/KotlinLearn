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
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth

class MainActivity : ComponentActivity() {

    //private val log: Logger = LoggerFactory.getLogger(MainActivity::class.java)
    override fun onCreate(savedInstanceState: Bundle?) {
        val log = LoggerFactory.getLogger(MainActivity::class.java);
        super.onCreate(savedInstanceState)
        log.info("Приложение запущено")
        val fileStorage = FileStorage(File(this.filesDir, "todo_items.json"))

        val item1 = TodoItem(text = "Проснуться", color = Color.Red,
            priority = Priority.CRITICAL, deadline = LocalDateTime.now().plusSeconds(5))
        fileStorage.add(item1)
        val item2 = TodoItem(text = "Потянуться", priority = Priority.STANDARD)
        fileStorage.add(item2)
        val item3 = TodoItem(text = "Поспать", priority = Priority.MINOR, color = Color.Yellow,
            deadline = LocalDateTime.now().plusMinutes(5))
        fileStorage.add(item3)


        log.debug("После добавления: ${fileStorage.items}")

        fileStorage.saveToFile()
        Thread.sleep(6000)

        log.debug("После дедлайна: ${fileStorage.items}")

        fileStorage.remove(item3.uid).let {
            log.debug("После удаления: ${fileStorage.items}")
        }

        fileStorage.loadFromFile()
        log.debug("После загрузки: ${fileStorage.items}")
        setContent {
            ToDosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Yellow
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Заголовок
                        Text(
                            text = "Будущий TODOList (оч крутой)",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Задача 1
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.medium,
                            shadowElevation = 4.dp, // Тень для объема
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Задача 1",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp),
                                color =Color.Black
                            )
                        }

                        // Задача 2
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.medium,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Задача 2",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp),
                                color = Color.Black
                            )
                        }

                        // Задача 3
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.medium,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Задача 3",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp),
                                color = Color.Black
                            )
                        }
                    }
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