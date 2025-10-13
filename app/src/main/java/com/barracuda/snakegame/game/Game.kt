package com.barracuda.snakegame.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.barracuda.snakegame.R
import com.barracuda.snakegame.ui.theme.*
import com.barracuda.snakegame.util.GameConstants.BASE_DELAY_MS
import com.barracuda.snakegame.util.GameConstants.BOARD_SIZE
import com.barracuda.snakegame.util.GameConstants.DELAY_DECREASE_PER_FOOD_MS
import com.barracuda.snakegame.util.GameConstants.INITIAL_SNAKE_LENGTH
import com.barracuda.snakegame.util.GameConstants.MIN_DELAY_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

data class SnakeSegment(val position: Pair<Int, Int>, val color: Color)

data class State(
    val targetLetterPosition: Pair<Int, Int>,
    val targetLetter: Char,
    val targetLetterColor: Color,
    val distractorLetterPosition: Pair<Int, Int>,
    val distractorLetter: Char,
    val distractorLetterColor: Color,
    val snake: List<SnakeSegment>,
    val score: Int,
    val speed: Int = 0,
    val eatenLetterColors: Map<Char, Color> = emptyMap(),
    val highScore: Int = 0,
    val isGameOver: Boolean = false
)

class Game(private val scope: CoroutineScope, private val context: Context) {

    private val foodColors: List<Color> = listOf(
        FoodRed,
        FoodBlue,
        FoodYellow,
        FoodMagenta,
        UiText, // White
        FoodOrange,
        FoodCyan
    )
    private val random = Random.Default

    private lateinit var soundPool: SoundPool
    private var gameStartSoundId: Int = 0
    private var correctEatSoundId: Int = 0
    private var wrongEatSoundId: Int = 0
    private var gameOverSoundId: Int = 0

    private val mutableIsPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = mutableIsPaused

    private val mutableIsGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = mutableIsGameActive

    private var highScore: Int = 0
    private val mutex = Mutex()

    var move = Pair(1, 0)
        set(value) {
            scope.launch {
                mutex.withLock {
                    field = value
                }
            }
        }

    private fun generateRandomLetter(exclude: Char? = null): Char {
        var letter: Char
        do {
            letter = ('a'..'z').random(random)
        } while (letter == exclude)
        return letter
    }

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
        val headStartPos = Pair(7, 7)
        for (i in 0 until INITIAL_SNAKE_LENGTH) {
            initialSnakeBodySegments.add(
                SnakeSegment(
                    Pair(headStartPos.first - i, headStartPos.second),
                    INITIAL_SNAKE_COLORS[i % INITIAL_SNAKE_COLORS.size]
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
            score = 0,
            speed = 0,
            eatenLetterColors = emptyMap(),
            highScore = this.highScore,
            isGameOver = false
        )
    }

    private val mutableState = MutableStateFlow(createInitialGameState())
    val state: StateFlow<State> = mutableState

    fun togglePause() {
        mutableIsPaused.value = !mutableIsPaused.value
    }

    fun startGame() {
        playSound(gameStartSoundId)
        mutableIsGameActive.value = true
        mutableIsPaused.value = false
        mutableState.value = createInitialGameState()
        move = Pair(1, 0)
    }

    fun restartGame() {
        startGame()
    }

    fun returnToMenu() {
        mutableIsGameActive.value = false
        mutableState.update { it.copy(isGameOver = false) }
    }

    private fun loadSounds() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        gameStartSoundId = soundPool.load(context, R.raw.game_start, 1)
        correctEatSoundId = soundPool.load(context, R.raw.correct_eat, 1)
        wrongEatSoundId = soundPool.load(context, R.raw.wrong_eat, 1)
        gameOverSoundId = soundPool.load(context, R.raw.game_over, 1)
        Log.d("GameSounds", "CorrectEatSoundId: $correctEatSoundId, WrongEatSoundId: $wrongEatSoundId, GameOverSoundId: $gameOverSoundId")
    }

    private fun playSound(soundId: Int) {
        if (soundId != 0) {
            soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
        }
    }

    init {
        loadSounds()
        val prefs = context.getSharedPreferences(SNAKE_GAME_PREFS, Context.MODE_PRIVATE)
        highScore = prefs.getInt(HIGH_SCORE_KEY, 0)
        mutableState.value = createInitialGameState()

        scope.launch {
            gameLoop@ while (true) {
                if (!isGameActive.value) {
                    delay(100L)
                    continue@gameLoop
                }

                if (mutableIsPaused.value) {
                    delay(100L)
                    continue@gameLoop
                }

                val foodEaten = mutableState.value.snake.size - INITIAL_SNAKE_LENGTH
                val currentDelay = BASE_DELAY_MS - (foodEaten * DELAY_DECREASE_PER_FOOD_MS)
                val actualDelay = currentDelay.coerceAtLeast(MIN_DELAY_MS)
                val speed = ((BASE_DELAY_MS - actualDelay) * 100 / (BASE_DELAY_MS - MIN_DELAY_MS)).toInt()

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
                    var newScore = currentState.score
                    var updatedEatenLetterColors = currentState.eatenLetterColors

                    var newHeadColor: Color
                    var nextSnakeBody: List<SnakeSegment>
                    var newSnakeLength = currentState.snake.size

                    if (newHeadPosition == currentState.targetLetterPosition) {
                        newSnakeLength++
                        newScore += 5
                        playSound(correctEatSoundId)
                        updatedEatenLetterColors = currentState.eatenLetterColors +
                                (currentState.targetLetter to currentState.targetLetterColor)
                        newHeadColor = currentState.targetLetterColor

                        val newHeadSegment = SnakeSegment(newHeadPosition, newHeadColor)
                        nextSnakeBody = listOf(newHeadSegment) + currentState.snake.take(newSnakeLength - 1)

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
                        nextSnakeBody = listOf(newHeadSegment) + currentState.snake.take(newSnakeLength - 1)

                        nextDistractorLetter = generateRandomLetter(exclude = currentState.targetLetter)
                        nextDistractorLetterColor = foodColors.random(random)
                        nextDistractorLetterPosition =
                            generateRandomSafePosition(nextSnakeBody, currentState.targetLetterPosition)

                    } else if (currentState.snake.any { it.position == newHeadPosition }) { // Self-collision check
                        checkAndSaveHighScore(currentState.score)
                        playSound(gameOverSoundId)
                        mutableIsGameActive.value = false
                        return@update currentState.copy(
                            isGameOver = true,
                            highScore = this@Game.highScore
                        )
                    } else { // Normal move
                        newHeadColor = currentState.snake.first().color
                        val newHeadSegment = SnakeSegment(newHeadPosition, newHeadColor)
                        nextSnakeBody = listOf(newHeadSegment) + currentState.snake.take(newSnakeLength - 1)
                    }

                    currentState.copy(
                        targetLetterPosition = nextTargetLetterPosition,
                        targetLetter = nextTargetLetter,
                        targetLetterColor = nextTargetLetterColor,
                        distractorLetterPosition = nextDistractorLetterPosition,
                        distractorLetter = nextDistractorLetter,
                        distractorLetterColor = nextDistractorLetterColor,
                        snake = nextSnakeBody,
                        score = newScore,
                        speed = speed,
                        eatenLetterColors = updatedEatenLetterColors,
                        highScore = currentState.highScore
                    )
                }
            }
        }
    }

    companion object {
        private val INITIAL_SNAKE_COLORS = listOf(SnakeHeadColor)
        private const val SNAKE_GAME_PREFS = "SnakeGamePrefs"
        private const val HIGH_SCORE_KEY = "HighScore"
    }
}