package com.barracuda.snakegame

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.shape.RoundedCornerShape // Added for button shapes
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.ButtonDefaults // Added for button elevation
import androidx.compose.material.Text // Added import
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember // Added for remembering activity instance
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue // Added for 'by' delegate with collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Added for food color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp // Added for score text size
import androidx.compose.ui.platform.LocalContext // Added for accessing activity context
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

// Define SnakeSegment data class
data class SnakeSegment(val position: Pair<Int, Int>, val color: Color)

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
    val snake: List<SnakeSegment>, // Changed from List<Pair<Int, Int>>
    val score: Int, // Added score
    val eatenLetters: Set<Char> = emptySet(), // Added to track eaten target letters
    val highScore: Int = 0, // Added for high score display
    val isGameOver: Boolean = false // Added for game over state
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
    private var gameOverSoundId: Int = 0

    private val mutableIsPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = mutableIsPaused

    private var highScore: Int = 0
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
        currentSnakeSegments: List<SnakeSegment>,
        vararg occupiedSpots: Pair<Int, Int>
    ): Pair<Int, Int> {
        var position: Pair<Int, Int>
        val snakePositions = currentSnakeSegments.map { it.position }.toSet()
        val allOccupied = snakePositions + occupiedSpots.filterNotNull().toSet()
        do {
            position = Pair(random.nextInt(BOARD_SIZE), random.nextInt(BOARD_SIZE))
        } while (allOccupied.contains(position))
        return position
    }

    private fun checkAndSaveHighScore(currentScore: Int) {
        if (currentScore > highScore) {
            highScore = currentScore
            val prefs = context.getSharedPreferences(SNAKE_GAME_PREFS, Context.MODE_PRIVATE)
            with(prefs.edit()) {
                putInt(HIGH_SCORE_KEY, highScore)
                apply()
            }
        }
    }
    private fun createInitialGameState(): State {
        val initialSnakeBodySegments = mutableListOf<SnakeSegment>()
        val headStartPos = Pair(7, 7) // Assuming initial move is to the right (1,0)
        for (i in 0 until INITIAL_SNAKE_LENGTH) {
            initialSnakeBodySegments.add(
                SnakeSegment(
                    Pair(headStartPos.first - i, headStartPos.second),
                    INITIAL_SNAKE_COLORS[i % INITIAL_SNAKE_COLORS.size] // Use modulo for safety if length > colors
                )
            )
        }

        val initialTargetLetter = 'a'
        val initialTargetLetterColor = foodColors.random(random)
        val initialTargetLetterPosition = generateRandomSafePosition(initialSnakeBodySegments)
        val initialDistractorLetter = generateRandomLetter(exclude = initialTargetLetter)
        val initialDistractorLetterColor = foodColors.random(random)
        val initialDistractorLetterPosition =
            generateRandomSafePosition(initialSnakeBodySegments, initialTargetLetterPosition)
        return State(
            targetLetterPosition = initialTargetLetterPosition,
            targetLetter = initialTargetLetter,
            targetLetterColor = initialTargetLetterColor,
            distractorLetterPosition = initialDistractorLetterPosition,
            distractorLetter = initialDistractorLetter,
            distractorLetterColor = initialDistractorLetterColor,
            snake = initialSnakeBodySegments,
            score = 0, // Initialize score
            eatenLetters = emptySet(), // Explicitly initialize, though default works
            highScore = this.highScore, // Initialize with current high score from Game class
            isGameOver = false // Ensure game starts not over
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

    fun restartGame() {
        mutableIsPaused.value = false // Ensure game is not paused on restart
        mutableState.value = createInitialGameState()
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
        gameOverSoundId = soundPool.load(context, R.raw.game_over, 1)
        Log.d("GameSounds", "CorrectEatSoundId: $correctEatSoundId, WrongEatSoundId: $wrongEatSoundId, GameOverSoundId: $gameOverSoundId")
    }

    private fun playSound(soundId: Int) {
        if (soundId != 0) { // Check if sound loaded successfully
            soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
        }
    }

    init {
        loadSounds() // Load sounds when Game is initialized
        // Load high score
        val prefs = context.getSharedPreferences(SNAKE_GAME_PREFS, Context.MODE_PRIVATE)
        highScore = prefs.getInt(HIGH_SCORE_KEY, 0)
        mutableState.value = createInitialGameState() // Re-initialize state with loaded high score

        scope.launch {
            var snakeLength = INITIAL_SNAKE_LENGTH
            var currentScore = 0 // Local score tracking within the game loop

            gameLoop@ while (true) { // Added label for breaking out if needed
                if (mutableIsPaused.value) {
                    delay(100L) // Small delay to prevent a busy loop while paused
                    continue
                }
                // val foodEaten = snakeLength - INITIAL_SNAKE_LENGTH // Not strictly needed for delay if simplified
                val foodEaten = snakeLength - INITIAL_SNAKE_LENGTH //Re-added this line, because it is used below.
                val currentDelay = BASE_DELAY_MS - (foodEaten * DELAY_DECREASE_PER_FOOD_MS)
                val actualDelay = currentDelay.coerceAtLeast(MIN_DELAY_MS)

                delay(actualDelay)
                mutableState.update { currentState -> // currentState.snake is List<SnakeSegment>
                    if (currentState.isGameOver) { // Don't update game logic if game is over
                        return@update currentState
                    }

                    val newHeadPosition = currentState.snake.first().position.let { poz ->
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
                    var updatedEatenLetters = currentState.eatenLetters
                    
                    var newHeadColor: Color
                    var nextSnakeBody: List<SnakeSegment>

                    if (newHeadPosition == currentState.targetLetterPosition) {
                        snakeLength++
                        newScore += 5
                        playSound(correctEatSoundId)
                        updatedEatenLetters = currentState.eatenLetters + currentState.targetLetter
                        newHeadColor = currentState.targetLetterColor

                        val newHeadSegment = SnakeSegment(newHeadPosition, newHeadColor)
                        nextSnakeBody = listOf(newHeadSegment) + currentState.snake.take(snakeLength - 1)

                        nextTargetLetter =
                            if (currentState.targetLetter == 'z') 'a' else currentState.targetLetter + 1
                        nextTargetLetterColor = foodColors.random(random)
                        nextTargetLetterPosition = generateRandomSafePosition(nextSnakeBody)

                        nextDistractorLetter = generateRandomLetter(exclude = nextTargetLetter)
                        nextDistractorLetterColor = foodColors.random(random)
                        nextDistractorLetterPosition =
                            generateRandomSafePosition(nextSnakeBody, nextTargetLetterPosition)

                    } else if (newHeadPosition == currentState.distractorLetterPosition) {
                        newScore -= 5
                        playSound(wrongEatSoundId)
                        newHeadColor = currentState.distractorLetterColor
                        
                        val newHeadSegment = SnakeSegment(newHeadPosition, newHeadColor)
                        nextSnakeBody = listOf(newHeadSegment) + currentState.snake.take(snakeLength - 1) // Length doesn't change, last element effectively dropped
                        
                        nextDistractorLetter = generateRandomLetter(exclude = currentState.targetLetter)
                        nextDistractorLetterColor = foodColors.random(random)
                        nextDistractorLetterPosition =
                            generateRandomSafePosition(nextSnakeBody, currentState.targetLetterPosition)

                    } else if (currentState.snake.any { it.position == newHeadPosition }) { // Self-collision check
                        checkAndSaveHighScore(currentScore) // Save score
                        playSound(gameOverSoundId) // Play game over sound
                        snakeLength = INITIAL_SNAKE_LENGTH // Reset local variable for next game
                        return@update currentState.copy(
                            isGameOver = true,
                            highScore = this@Game.highScore // Ensure high score is updated in the state
                        )
                    } else { // Normal move
                        newHeadColor = currentState.snake.first().color // Inherit color from old head
                        val newHeadSegment = SnakeSegment(newHeadPosition, newHeadColor)
                        nextSnakeBody = listOf(newHeadSegment) + currentState.snake.take(snakeLength - 1)
                    }

                    currentScore = newScore // Update local score tracker

                    currentState.copy(
                        targetLetterPosition = nextTargetLetterPosition,
                        targetLetter = nextTargetLetter,
                        targetLetterColor = nextTargetLetterColor,
                        distractorLetterPosition = nextDistractorLetterPosition,
                        distractorLetter = nextDistractorLetter,
                        distractorLetterColor = nextDistractorLetterColor,
                        snake = nextSnakeBody,
                        score = newScore,
                        eatenLetters = updatedEatenLetters,
                        highScore = currentState.highScore // Keep displaying the high score from current state
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
        private val INITIAL_SNAKE_COLORS = listOf(
            DarkGreen, // From ui.theme
            Color.Red,
            Color.Yellow,
            Color.White
        )
        private const val SNAKE_GAME_PREFS = "SnakeGamePrefs"
        private const val HIGH_SCORE_KEY = "HighScore"
    }
}

@Composable
fun Snake(game: Game) {
    val gameState by game.state.collectAsState()
    val isPaused by game.isPaused.collectAsState() // Still needed for Pause button text

    // Get activity context for finishing the app
    val activity = (LocalContext.current as? ComponentActivity)

    if (gameState.isGameOver) {
        GameOverScreen(
            finalScore = gameState.score,
            highScore = gameState.highScore,
            onRestart = { game.restartGame() },
            onExit = { activity?.finish() }
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "High: ${gameState.highScore}", fontSize = 20.sp)
                Text(text = "Score: ${gameState.score}", fontSize = 20.sp)
            }
            Board(gameState)
            AlphabetDisplay(eatenLetters = gameState.eatenLetters)
            Buttons { direction ->
                if (!isPaused) { // Only allow direction changes if not paused
                    game.move = direction
                }
            }
            Button(
                onClick = { game.togglePause() },
                modifier = Modifier.padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
            ) {
                Text(if (isPaused) "Resume" else "Pause")
            }
        }
    }
}

@Composable
fun GameOverScreen(finalScore: Int, highScore: Int, onRestart: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("GAME OVER", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.error)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your Score: $finalScore", fontSize = 24.sp)
        Text("High Score: $highScore", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Restart")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Exit")
        }
    }
}

@Composable
fun Buttons(onDirectionChange: (Pair<Int, Int>) -> Unit) {
    val buttonSize = Modifier.size(64.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Button(
            onClick = { onDirectionChange(Pair(0, -1)) },
            modifier = buttonSize,
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, null)
        }
        Row {
            Button(
                onClick = { onDirectionChange(Pair(-1, 0)) },
                modifier = buttonSize,
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, null)
            }
            Spacer(modifier = buttonSize)
            Button(
                onClick = { onDirectionChange(Pair(1, 0)) },
                modifier = buttonSize,
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowRight, null)
            }
        }
        Button(onClick = { onDirectionChange(Pair(0, 1)) }, modifier = buttonSize, shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, null)
        }
    }
}

// Board composable is now in Board.kt
// @Composable
// fun Board(state: State) { ... }

@Composable
fun AlphabetDisplay(eatenLetters: Set<Char>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heightIn(min = 20.dp), // Ensure the Row has some height even if empty
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally) // Center group, space letters
    ) {
        val sortedEatenUppercase = eatenLetters.map { it.uppercaseChar() }.sorted()

        if (sortedEatenUppercase.isEmpty()) {
            // Optionally, display a placeholder or leave empty. 
            // Adding a Text with an empty string or a specific placeholder if desired.
            // For now, it will just be an empty Row that takes up minimal space due to heightIn.
        } else {
            for (letterToShow in sortedEatenUppercase) {
                Text(
                    text = letterToShow.toString(),
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp // Can be a bit larger now
                )
            }
        }
    }
}