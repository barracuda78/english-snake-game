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
import androidx.compose.ui.unit.sp
import com.barracuda.snakegame.Board
import com.barracuda.snakegame.game.Game

// Define the gradient brush to be used by buttons
private val diagonalGradientBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF557eaa), Color(0xFF337dcc)),
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
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "High: ${gameState.highScore}", fontSize = 20.sp, color = Color.White)
                    Text(text = "Score: ${gameState.score}", fontSize = 20.sp, color = Color.White)
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
                .background(Color.Black.copy(alpha = 0.7f))
                .pointerInput(Unit) {},
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
        Spacer(modifier = Modifier.height(16.dp))
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
                    color = Color.White,
                    textAlign = TextAlign.Center,
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
                Text("Exit to Menu")
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

@Composable
fun AlphabetDisplay(eatenLetterColors: Map<Char, Color>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heightIn(min = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        ('A'..'Z').forEach { char ->
            val lowerChar = char.lowercaseChar()
            val letterColor = eatenLetterColors[lowerChar] ?: Color.Transparent
            Text(
                text = char.toString(),
                color = letterColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}