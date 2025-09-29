package com.barracuda.snakegame

import android.content.Context
import androidx.compose.foundation.background
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.shape.RoundedCornerShape // Added for button shapes
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
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
import androidx.compose.runtime.mutableStateOf // Added for AlertDialog state
import androidx.compose.runtime.setValue // Added for AlertDialog state
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color // Added for food color
import androidx.compose.ui.input.pointer.pointerInput // Added for PausedOverlay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

// Define the gradient brush to be used by buttons
private val diagonalGradientBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF557eaa), Color(0xFF337dcc)),
    start = Offset(0f, Float.POSITIVE_INFINITY), // Bottom-left
    end = Offset(Float.POSITIVE_INFINITY, 0f)    // Top-right
)
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
                    Snake(game) // This will now handle showing StartMenu or Game or GameOver
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
    val eatenLetterColors: Map<Char, Color> = emptyMap(), // Changed from eatenLetters: Set<Char>
    val highScore: Int = 0, // Added for high score display
    val isGameOver: Boolean = false // Added for game over state
)

class Game(private val scope: CoroutineScope, private val context: Context) {

    private val foodColors: List<Color> = listOf(
        Color.Red,
        Color(0xFF00FF00), // Bright Green (replaces Bright Blue)
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

    private val mutableIsGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = mutableIsGameActive

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
            eatenLetterColors = emptyMap(), // Initialize as empty map
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

    fun startGame() {
        mutableIsGameActive.value = true
        mutableIsPaused.value = false
        mutableState.value = createInitialGameState() // Reset game to initial state
        move = Pair(1,0) // Reset move direction
    }

    fun restartGame() {
        startGame() // Restarting is essentially starting the game again
    }

    fun returnToMenu() {
        mutableIsGameActive.value = false
        mutableState.update { it.copy(isGameOver = false) } // Ensure game over is reset for menu
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
        mutableState.value = createInitialGameState() // Initialize state with loaded high score

        scope.launch {
            var snakeLength = INITIAL_SNAKE_LENGTH
            var currentScore = 0 // Local score tracking within the game loop

            gameLoop@ while (true) {
                if (!isGameActive.value) { // If game is not active (e.g., at start menu)
                    delay(100L) // Idle delay
                    continue@gameLoop
                }

                if (mutableIsPaused.value) {
                    delay(100L) // Small delay to prevent a busy loop while paused
                    continue@gameLoop
                }
                
                val foodEaten = snakeLength - INITIAL_SNAKE_LENGTH 
                val currentDelay = BASE_DELAY_MS - (foodEaten * DELAY_DECREASE_PER_FOOD_MS)
                val actualDelay = currentDelay.coerceAtLeast(MIN_DELAY_MS)

                delay(actualDelay)
                mutableState.update { currentState -> 
                    if (currentState.isGameOver) { 
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
                    var updatedEatenLetterColors = currentState.eatenLetterColors // Changed from updatedEatenLetters
                    
                    var newHeadColor: Color
                    var nextSnakeBody: List<SnakeSegment>

                    if (newHeadPosition == currentState.targetLetterPosition) {
                        snakeLength++
                        newScore += 5
                        playSound(correctEatSoundId)
                        // Add the eaten letter and its color to the map
                        updatedEatenLetterColors = currentState.eatenLetterColors + 
                                (currentState.targetLetter to currentState.targetLetterColor)
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
                        nextSnakeBody = listOf(newHeadSegment) + currentState.snake.take(snakeLength - 1) 
                        
                        nextDistractorLetter = generateRandomLetter(exclude = currentState.targetLetter)
                        nextDistractorLetterColor = foodColors.random(random)
                        nextDistractorLetterPosition =
                            generateRandomSafePosition(nextSnakeBody, currentState.targetLetterPosition)

                    } else if (currentState.snake.any { it.position == newHeadPosition }) { // Self-collision check
                        checkAndSaveHighScore(currentScore) 
                        playSound(gameOverSoundId) 
                        mutableIsGameActive.value = false // Game is no longer active
                        snakeLength = INITIAL_SNAKE_LENGTH 
                        return@update currentState.copy(
                            isGameOver = true,
                            highScore = this@Game.highScore 
                        )
                    } else { // Normal move
                        newHeadColor = currentState.snake.first().color 
                        val newHeadSegment = SnakeSegment(newHeadPosition, newHeadColor)
                        nextSnakeBody = listOf(newHeadSegment) + currentState.snake.take(snakeLength - 1)
                    }

                    currentScore = newScore 

                    currentState.copy(
                        targetLetterPosition = nextTargetLetterPosition,
                        targetLetter = nextTargetLetter,
                        targetLetterColor = nextTargetLetterColor,
                        distractorLetterPosition = nextDistractorLetterPosition,
                        distractorLetter = nextDistractorLetter,
                        distractorLetterColor = nextDistractorLetterColor,
                        snake = nextSnakeBody,
                        score = newScore,
                        eatenLetterColors = updatedEatenLetterColors, // Pass the updated map
                        highScore = currentState.highScore 
                    )
                }
            }
        }
    }

    companion object {
        const val BOARD_SIZE = 32
        const val INITIAL_SNAKE_LENGTH = 4     
        const val BASE_DELAY_MS = 200L         
        const val MIN_DELAY_MS = 50L           
        const val DELAY_DECREASE_PER_FOOD_MS = 5L 
        private val INITIAL_SNAKE_COLORS = listOf(Color(0xFF00ffee)) 
        private const val SNAKE_GAME_PREFS = "SnakeGamePrefs"
        private const val HIGH_SCORE_KEY = "HighScore"
    }
}

@Composable
fun Snake(game: Game) {
    val gameState by game.state.collectAsState()
    val isPaused by game.isPaused.collectAsState()
    val isGameActive by game.isGameActive.collectAsState()

    when {
        !isGameActive && !gameState.isGameOver -> {
            StartMenuScreen(onStartClick = { game.startGame() })
        }
        isGameActive && !gameState.isGameOver -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "High score: ${gameState.highScore}", fontSize = 20.sp, color = Color.White)
                    Text(text = "Score: ${gameState.score}", fontSize = 20.sp, color = Color.White)
                }
                
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f), // Added modifier
                    contentAlignment = Alignment.Center
                ) {
                    Board(gameState)
                    PausedOverlay(isVisible = isPaused)
                }

                AlphabetDisplay(eatenLetterColors = gameState.eatenLetterColors) // Pass the map
                Buttons { direction ->
                    if (!isPaused) { 
                        game.move = direction
                    }
                }
                Button(
                    onClick = { game.togglePause() },
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(0.5f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent, 
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp) 
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isPaused) "Resume" else "Pause")
                    }
                }
            }
        }
        gameState.isGameOver -> {
            GameOverScreen(
                finalScore = gameState.score,
                highScore = gameState.highScore,
                onRestart = { game.restartGame() },
                onExit = { game.returnToMenu() } // Changed from activity?.finish()
            )
        }
    }
}

@Composable
fun PausedOverlay(isVisible: Boolean) {
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .pointerInput(Unit) {}, // Consume touch events when paused to prevent interaction with underlying elements
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PAUSED",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StartMenuScreen(onStartClick: () -> Unit) {
    var showInfoDialog by remember { mutableStateOf(false) }
    val activity = (LocalContext.current as? ComponentActivity)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Snake Game", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(64.dp))
        Button(
            onClick = onStartClick,
            modifier = Modifier.fillMaxWidth(0.6f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Start", fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) 
        Button(
            onClick = { showInfoDialog = true },
            modifier = Modifier.fillMaxWidth(0.6f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Info", fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Spacer between Info and Exit buttons
        Button(
            onClick = { activity?.finish() },
            modifier = Modifier.fillMaxWidth(0.6f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Exit", fontSize = 20.sp)
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Game Information", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(
                    "ABC Snake game.\nVersion 0.1.13.\nCreated by Andrei Ruzaev.\nCopyright 2025",
                    color = Color.White, // Explicitly set for clarity
                    textAlign = TextAlign.Center, // Center align the body text as well
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showInfoDialog = false },
                        modifier = Modifier.fillMaxWidth(0.6f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("OK")
                        }
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            backgroundColor = MaterialTheme.colors.background // Set dialog background color
        )
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
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(0.6f).height(48.dp),
            shape = RoundedCornerShape(12.dp), 
            elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp), 
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Restart")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(0.6f).height(48.dp),
            shape = RoundedCornerShape(12.dp), 
            elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp), 
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Exit to Menu") // Changed text for clarity
            }
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
            elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowUp, null)
            }
        }
        Row {
            Button(
                onClick = { onDirectionChange(Pair(-1, 0)) },
                modifier = buttonSize,
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, null)
                }
            }
            Spacer(modifier = buttonSize)
            Button(
                onClick = { onDirectionChange(Pair(1, 0)) },
                modifier = buttonSize,
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, null)
                }
            }
        }
        Button(
            onClick = { onDirectionChange(Pair(0, 1)) },
            modifier = buttonSize,
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowDown, null)
            }
        }
    }
}

// Board composable is now in Board.kt
// @Composable
// fun Board(state: State) { ... }

@Composable
fun AlphabetDisplay(eatenLetterColors: Map<Char, Color>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heightIn(min = 20.dp), // Ensure the Row has some height even if empty
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        ('A'..'Z').forEach { char ->
            val lowerChar = char.lowercaseChar()
            val letterColor = eatenLetterColors[lowerChar] ?: Color.Transparent // Use food color or Transparent
            Text(
                text = char.toString(),
                color = letterColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}