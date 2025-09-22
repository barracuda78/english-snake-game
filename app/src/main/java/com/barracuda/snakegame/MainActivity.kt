package com.barracuda.snakegame

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color // Added for food color
import androidx.compose.ui.unit.sp // Added for score text size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.barracuda.snakegame.R
import com.barracuda.snakegame.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow // Added import
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log // Added for logging
// import java.util.* // Prefer Kotlin's Random
import kotlin.random.Random // Explicit import for clarity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val game = Game(lifecycleScope, this)

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

data class State(
    val targetLetterPosition: Pair<Int, Int>,
    val targetLetter: Char,
    val targetLetterColor: Color,
    val distractorLetterPosition: Pair<Int, Int>,
    val distractorLetter: Char,
    val distractorLetterColor: Color,
    val snake: List<Pair<Int, Int>>,
    val score: Int // Added score
)

class Game(private val scope: CoroutineScope, private val context: Context) {

    private val foodColors: List<Color> = listOf(
        Color.Red,
        Color(0xFF007BFF), // Bright Blue
        Color.Yellow,
        Color.Magenta,
        Color.White,
        Color(0xFFFFA500), // Orange
        Color.Cyan
    )
    private val random = Random.Default // Use Kotlin's Random

    private lateinit var soundPool: SoundPool
    private var correctEatSoundId: Int = 0
    private var wrongEatSoundId: Int = 0

    private val mutableIsPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = mutableIsPaused

    private val mutex = Mutex()

    // Helper to generate a random letter, optionally excluding one
    private fun generateRandomLetter(exclude: Char? = null): Char {
        var letter: Char
        do {
            letter = ('a'..'z').random(random)
        } while (letter == exclude)
        return letter
    }

    // Helper to generate a safe position for food
    private fun generateRandomSafePosition(
        currentSnake: List<Pair<Int, Int>>,
        vararg occupiedSpots: Pair<Int, Int>
    ): Pair<Int, Int> {
        var position: Pair<Int, Int>
        val allOccupied = currentSnake.toSet() + occupiedSpots.filterNotNull().toSet()
        do {
            position = Pair(random.nextInt(BOARD_SIZE), random.nextInt(BOARD_SIZE))
        } while (allOccupied.contains(position))
        return position
    }

    private fun createInitialGameState(): State {
        val initialSnakeBody = listOf(Pair(7, 7)) // Snake starts as a single point, grows in the loop
        val initialTargetLetter = 'a'
        val initialTargetLetterColor = foodColors.random(random)
        val initialTargetLetterPosition = generateRandomSafePosition(initialSnakeBody)
        val initialDistractorLetter = generateRandomLetter(exclude = initialTargetLetter)
        val initialDistractorLetterColor = foodColors.random(random)
        val initialDistractorLetterPosition =
            generateRandomSafePosition(initialSnakeBody, initialTargetLetterPosition)
        return State(
            targetLetterPosition = initialTargetLetterPosition,
            targetLetter = initialTargetLetter,
            targetLetterColor = initialTargetLetterColor,
            distractorLetterPosition = initialDistractorLetterPosition,
            distractorLetter = initialDistractorLetter,
            distractorLetterColor = initialDistractorLetterColor,
            snake = initialSnakeBody,
            score = 0 // Initialize score
        )
    }

    private val mutableState = MutableStateFlow(createInitialGameState())
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

    private fun loadSounds() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2) // Can play 2 sounds simultaneously
            .setAudioAttributes(audioAttributes)
            .build()

        correctEatSoundId = soundPool.load(context, R.raw.correct_eat, 1)
        wrongEatSoundId = soundPool.load(context, R.raw.wrong_eat, 1)
        Log.d("GameSounds", "CorrectEatSoundId: $correctEatSoundId, WrongEatSoundId: $wrongEatSoundId")
    }

    private fun playSound(soundId: Int) {
        if (soundId != 0) { // Check if sound loaded successfully
            soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
        }
    }

    init {
        loadSounds() // Load sounds when Game is initialized

        scope.launch {
            var snakeLength = INITIAL_SNAKE_LENGTH
            var currentScore = 0 // Local score tracking within the game loop

            while (true) {
                if (mutableIsPaused.value) {
                    delay(100L) // Small delay to prevent a busy loop while paused
                    continue
                }
                // val foodEaten = snakeLength - INITIAL_SNAKE_LENGTH // Not strictly needed for delay if simplified
                val foodEaten = snakeLength - INITIAL_SNAKE_LENGTH //Re-added this line, because it is used below.
                val currentDelay = BASE_DELAY_MS - (foodEaten * DELAY_DECREASE_PER_FOOD_MS)
                val actualDelay = currentDelay.coerceAtLeast(MIN_DELAY_MS)

                delay(actualDelay)
                mutableState.update { currentState ->
                    val newHeadPosition = currentState.snake.first().let { poz ->
                        mutex.withLock {
                            Pair(
                                (poz.first + move.first + BOARD_SIZE) % BOARD_SIZE,
                                (poz.second + move.second + BOARD_SIZE) % BOARD_SIZE
                            )
                        }
                    }

                    var nextTargetLetter = currentState.targetLetter
                    var nextTargetLetterColor = currentState.targetLetterColor
                    var nextTargetLetterPosition = currentState.targetLetterPosition

                    var nextDistractorLetter = currentState.distractorLetter
                    var nextDistractorLetterColor = currentState.distractorLetterColor
                    var nextDistractorLetterPosition = currentState.distractorLetterPosition
                    var newScore = currentScore

                    // Tentative next snake state based on current length, before checking collisions/eating
                    val potentialNextSnake = listOf(newHeadPosition) + currentState.snake.take(snakeLength - 1)

                    if (newHeadPosition == currentState.targetLetterPosition) {
                        snakeLength++
                        newScore += 5
                        playSound(correctEatSoundId)
                        // Snake grows, use its new full length for safe position generation
                        val grownSnake = listOf(newHeadPosition) + currentState.snake.take(snakeLength - 1)

                        nextTargetLetter =
                            if (currentState.targetLetter == 'z') 'a' else currentState.targetLetter + 1
                        nextTargetLetterColor = foodColors.random(random)
                        nextTargetLetterPosition = generateRandomSafePosition(grownSnake)

                        nextDistractorLetter = generateRandomLetter(exclude = nextTargetLetter)
                        nextDistractorLetterColor = foodColors.random(random)
                        nextDistractorLetterPosition =
                            generateRandomSafePosition(grownSnake, nextTargetLetterPosition)

                    } else if (newHeadPosition == currentState.distractorLetterPosition) {
                        newScore -= 5
                        playSound(wrongEatSoundId)
                        // Regenerate the distractor, snake does not grow, target does not change
                        nextDistractorLetter = generateRandomLetter(exclude = currentState.targetLetter)
                        nextDistractorLetterColor = foodColors.random(random)
                        nextDistractorLetterPosition =
                            generateRandomSafePosition(potentialNextSnake, currentState.targetLetterPosition)

                    } else if (currentState.snake.contains(newHeadPosition)) { // Self-collision
                        snakeLength = INITIAL_SNAKE_LENGTH
                        newScore = 0 // Reset score on collision
                        // Regenerate both food items to avoid spawning on them after reset
                        val resetSnake = listOf(newHeadPosition) + emptyList<Pair<Int,Int>>().take(snakeLength -1) // Simplified initial snake for placement

                        nextTargetLetter = 'a' // Reset target to 'a'
                        nextTargetLetterColor = foodColors.random(random)
                        nextTargetLetterPosition = generateRandomSafePosition(resetSnake)

                        nextDistractorLetter = generateRandomLetter(exclude = nextTargetLetter)
                        nextDistractorLetterColor = foodColors.random(random)
                        nextDistractorLetterPosition =
                            generateRandomSafePosition(resetSnake, nextTargetLetterPosition)
                    }

                    currentScore = newScore // Update local score tracker

                    currentState.copy(
                        targetLetterPosition = nextTargetLetterPosition,
                        targetLetter = nextTargetLetter,
                        targetLetterColor = nextTargetLetterColor,
                        distractorLetterPosition = nextDistractorLetterPosition,
                        distractorLetter = nextDistractorLetter,
                        distractorLetterColor = nextDistractorLetterColor,
                        snake = listOf(newHeadPosition) + currentState.snake.take(snakeLength - 1),
                        score = newScore
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

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp), // Added padding to the main column
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(text = "Score: ${gameState.score}", fontSize = 20.sp)
        }
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