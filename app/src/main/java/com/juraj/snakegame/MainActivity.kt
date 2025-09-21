package com.juraj.snakegame

import android.R.attr.maxWidth
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text // Added import
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue // Added for 'by' delegate with collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.juraj.snakegame.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow // Added import
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val game = Game(lifecycleScope)

        setContent {
            SnakeGameTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Snake(game)
                }
            }
        }
    }
}

data class State(val food: Pair<Int, Int>, val snake: List<Pair<Int, Int>>)

class Game(private val scope: CoroutineScope) {

    private val mutableIsPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = mutableIsPaused

    private val mutex = Mutex()
    private val mutableState =
        MutableStateFlow(State(food = Pair(5, 5), snake = listOf(Pair(7, 7))))
    val state: StateFlow<State> = mutableState // Changed Flow to StateFlow

    var move = Pair(1, 0)
        set(value) {
            scope.launch {
                mutex.withLock {
                    field = value
                }
            }
        }

    fun togglePause() {
        mutableIsPaused.value = !mutableIsPaused.value
    }

    init {
        scope.launch {
            var snakeLength = INITIAL_SNAKE_LENGTH

            while (true) {
                if (mutableIsPaused.value) {
                    delay(100L) // Small delay to prevent a busy loop while paused
                    continue
                }
                val foodEaten = snakeLength - INITIAL_SNAKE_LENGTH
                val currentDelay = BASE_DELAY_MS - (foodEaten * DELAY_DECREASE_PER_FOOD_MS)
                val actualDelay = currentDelay.coerceAtLeast(MIN_DELAY_MS)

                delay(actualDelay)
                mutableState.update {
                    val newPosition = it.snake.first().let { poz ->
                        mutex.withLock {
                            Pair(
                                (poz.first + move.first + BOARD_SIZE) % BOARD_SIZE,
                                (poz.second + move.second + BOARD_SIZE) % BOARD_SIZE
                            )
                        }
                    }

                    if (newPosition == it.food) {
                        snakeLength++
                    }

                    if (it.snake.contains(newPosition)) {
                        snakeLength = INITIAL_SNAKE_LENGTH // Reset to initial length
                    }

                    it.copy(
                        food = if (newPosition == it.food) Pair(
                            Random().nextInt(BOARD_SIZE),
                            Random().nextInt(BOARD_SIZE)
                        ) else it.food,
                        snake = listOf(newPosition) + it.snake.take(snakeLength - 1)
                    )
                }
            }
        }
    }

    companion object {
        const val BOARD_SIZE = 32
        const val INITIAL_SNAKE_LENGTH = 4     // Starting length of the snake
        const val BASE_DELAY_MS = 200L         // Delay at initial length (milliseconds)
        const val MIN_DELAY_MS = 50L           // Minimum delay (fastest speed)
        const val DELAY_DECREASE_PER_FOOD_MS = 5L // How much delay decreases per food item
    }
}

@Composable
fun Snake(game: Game) {
    val gameState by game.state.collectAsState()
    val isPaused by game.isPaused.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Board(gameState) // This will call the Board function from Board.kt
        Buttons {
            game.move = it
        }
        Button(
            onClick = { game.togglePause() },
            modifier = Modifier.padding(top = 16.dp) // Add some spacing
        ) {
            Text(if (isPaused) "Resume" else "Pause")
        }
    }

}

@Composable
fun Buttons(onDirectionChange: (Pair<Int, Int>) -> Unit) {
    val buttonSize = Modifier.size(64.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Button(onClick = { onDirectionChange(Pair(0, -1)) }, modifier = buttonSize) {
            Icon(Icons.Default.KeyboardArrowUp, null)
        }
        Row {
            Button(onClick = { onDirectionChange(Pair(-1, 0)) }, modifier = buttonSize) {
                Icon(Icons.Default.KeyboardArrowLeft, null)
            }
            Spacer(modifier = buttonSize)
            Button(onClick = { onDirectionChange(Pair(1, 0)) }, modifier = buttonSize) {
                Icon(Icons.Default.KeyboardArrowRight, null)
            }
        }
        Button(onClick = { onDirectionChange(Pair(0, 1)) }, modifier = buttonSize) {
            Icon(Icons.Default.KeyboardArrowDown, null)
        }
    }
}

// Board composable is now in Board.kt
// @Composable
// fun Board(state: State) { ... }