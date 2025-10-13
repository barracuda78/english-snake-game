package com.barracuda.snakegame.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.barracuda.snakegame.Board
import com.barracuda.snakegame.game.Game
import com.barracuda.snakegame.ui.theme.*
import com.barracuda.snakegame.util.GameConstants
import com.barracuda.snakegame.util.Strings

// Define the gradient brush to be used by buttons
private val diagonalGradientBrush = Brush.linearGradient(
    colors = listOf(ButtonGradient1, ButtonGradient2),
    start = Offset(0f, Float.POSITIVE_INFINITY), // Bottom-left
    end = Offset(Float.POSITIVE_INFINITY, 0f)    // Top-right
)

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
                modifier = Modifier.fillMaxSize().padding(GameConstants.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = GameConstants.ScorePaddingHorizontal, vertical = GameConstants.ScorePaddingVertical),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${Strings.HIGH_SCORE_LABEL}${gameState.highScore}", fontSize = GameConstants.ScoreFontSize, color = UiText)
                    Text(text = "${Strings.SPEED_LABEL}${gameState.speed}", fontSize = GameConstants.ScoreFontSize, color = UiText)
                    Text(text = "${Strings.SCORE_LABEL}${gameState.score}", fontSize = GameConstants.ScoreFontSize, color = UiText)
                }

                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Board(gameState)
                    PausedOverlay(isVisible = isPaused)
                }

                AlphabetDisplay(eatenLetterColors = gameState.eatenLetterColors)
                Buttons { direction ->
                    if (!isPaused) {
                        game.move = direction
                    }
                }
                Button(
                    onClick = { game.togglePause() },
                    modifier = Modifier.padding(top = GameConstants.PauseButtonPaddingTop).fillMaxWidth(GameConstants.PauseButtonWidth).height(GameConstants.ButtonHeight),
                    shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
                    elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = UiText
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isPaused) Strings.RESUME else Strings.PAUSE)
                    }
                }
                Button(
                    onClick = { game.returnToMenu() },
                    modifier = Modifier.padding(top = GameConstants.MenuButtonSpacerHeight).fillMaxWidth(GameConstants.PauseButtonWidth).height(GameConstants.ButtonHeight),
                    shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
                    elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = UiText
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(Strings.EXIT)
                    }
                }
            }
        }
        gameState.isGameOver -> {
            GameOverScreen(
                finalScore = gameState.score,
                highScore = gameState.highScore,
                onRestart = { game.restartGame() },
                onExit = { game.returnToMenu() }
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
                .background(PausedOverlay)
                .pointerInput(Unit) {},
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Strings.PAUSED,
                color = UiText,
                fontSize = GameConstants.PausedFontSize,
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
        Text(Strings.APP_NAME, fontSize = GameConstants.TitleFontSize, fontWeight = FontWeight.Bold, color = UiText)
        Spacer(modifier = Modifier.height(GameConstants.StartMenuSpacerHeight))
        Button(
            onClick = onStartClick,
            modifier = Modifier.fillMaxWidth(GameConstants.MenuButtonWidth).height(GameConstants.ButtonHeight),
            shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
            elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = UiText
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Text(Strings.START, fontSize = GameConstants.MenuButtonFontSize)
            }
        }
        Spacer(modifier = Modifier.height(GameConstants.MenuButtonSpacerHeight))
        Button(
            onClick = { showInfoDialog = true },
            modifier = Modifier.fillMaxWidth(GameConstants.MenuButtonWidth).height(GameConstants.ButtonHeight),
            shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
            elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = UiText
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Text(Strings.INFO, fontSize = GameConstants.MenuButtonFontSize)
            }
        }
        Spacer(modifier = Modifier.height(GameConstants.MenuButtonSpacerHeight))
        Button(
            onClick = { activity?.finish() },
            modifier = Modifier.fillMaxWidth(GameConstants.MenuButtonWidth).height(GameConstants.ButtonHeight),
            shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
            elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = UiText
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Text(Strings.EXIT, fontSize = GameConstants.MenuButtonFontSize)
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(Strings.GAME_INFORMATION_TITLE, fontWeight = FontWeight.Bold, color = UiText)
                }
            },
            text = {
                Text(
                    Strings.GAME_INFORMATION_BODY,
                    color = UiText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = GameConstants.DialogConfirmButtonPaddingBottom, start = GameConstants.DialogConfirmButtonPaddingHorizontal, end = GameConstants.DialogConfirmButtonPaddingHorizontal),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showInfoDialog = false },
                        modifier = Modifier.fillMaxWidth(GameConstants.MenuButtonWidth).height(GameConstants.ButtonHeight),
                        shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
                        elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = UiText
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(Strings.OK)
                        }
                    }
                }
            },
            shape = RoundedCornerShape(GameConstants.DialogCornerRadius),
            backgroundColor = MaterialTheme.colors.background
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
        Text(Strings.GAME_OVER, fontSize = GameConstants.GameOverTitleFontSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.error)
        Spacer(modifier = Modifier.height(GameConstants.GameOverSpacerHeight1))
        Text("${Strings.YOUR_SCORE_LABEL}$finalScore", fontSize = GameConstants.GameOverScoreFontSize, color = UiText)
        Text("${Strings.HIGH_SCORE_LABEL}$highScore", fontSize = GameConstants.ScoreFontSize, color = UiText)
        Spacer(modifier = Modifier.height(GameConstants.GameOverSpacerHeight2))
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(GameConstants.MenuButtonWidth).height(GameConstants.ButtonHeight),
            shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
            elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = UiText
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Text(Strings.RESTART)
            }
        }
        Spacer(modifier = Modifier.height(GameConstants.MenuButtonSpacerHeight))
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(GameConstants.MenuButtonWidth).height(GameConstants.ButtonHeight),
            shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
            elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = UiText
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Text(Strings.EXIT_TO_MENU)
            }
        }
    }
}

@Composable
fun Buttons(onDirectionChange: (Pair<Int, Int>) -> Unit) {
    val buttonSize = Modifier.size(GameConstants.ButtonSize)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(GameConstants.ButtonColumnPadding)) {
        Button(
            onClick = { onDirectionChange(Pair(0, -1)) },
            modifier = buttonSize,
            shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
            elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = UiText
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowUp, null)
            }
        }
        Row {
            Button(
                onClick = { onDirectionChange(Pair(-1, 0)) },
                modifier = buttonSize,
                shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
                elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = UiText
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, null)
                }
            }
            Spacer(modifier = buttonSize)
            Button(
                onClick = { onDirectionChange(Pair(1, 0)) },
                modifier = buttonSize,
                shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
                elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = UiText
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, null)
                }
            }
        }
        Button(
            onClick = { onDirectionChange(Pair(0, 1)) },
            modifier = buttonSize,
            shape = RoundedCornerShape(GameConstants.ButtonCornerRadius),
            elevation = ButtonDefaults.elevation(defaultElevation = GameConstants.ButtonElevationDefault, pressedElevation = GameConstants.ButtonElevationPressed),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = UiText
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = diagonalGradientBrush, shape = RoundedCornerShape(GameConstants.ButtonCornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowDown, null)
            }
        }
    }
}

@Composable
fun AlphabetDisplay(eatenLetterColors: Map<Char, Color>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GameConstants.AlphabetRowPaddingVertical)
            .heightIn(min = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(GameConstants.AlphabetLetterSpacing, Alignment.CenterHorizontally)
    ) {
        ('A'..'Z').forEach { char ->
            val lowerChar = char.lowercaseChar()
            val letterColor = eatenLetterColors[lowerChar] ?: Color.Transparent
            Text(
                text = char.toString(),
                color = letterColor,
                fontWeight = FontWeight.Bold,
                fontSize = GameConstants.AlphabetFontSize
            )
        }
    }
}