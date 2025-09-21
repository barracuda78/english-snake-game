package com.juraj.snakegame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.material.Text // Added for food letter
import androidx.compose.ui.Alignment // Added for text alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight // Added for bold text
import androidx.compose.ui.text.style.TextAlign // Added for text alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // For text size
import com.juraj.snakegame.ui.theme.DarkGreen
import com.juraj.snakegame.ui.theme.Shapes

@Composable
fun Board(state: State) {
    BoxWithConstraints(Modifier.padding(2.dp)) {
        val tileSize = maxWidth / Game.BOARD_SIZE

        Box(
            Modifier
                .size(maxWidth)
                .border(2.dp, DarkGreen)
        )

        // Display the food letter
        Text(
            text = state.currentFoodLetter.uppercase(), // Changed to uppercase
            modifier = Modifier
                .offset(x = tileSize * state.foodPosition.first, y = tileSize * state.foodPosition.second)
                .size(tileSize),
            color = DarkGreen, // Use the same color as the snake/border for consistency
            fontSize = (tileSize.value * 0.9).sp, // Make font size relative to tile size (Increased from 0.7)
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold // Added for bold text
        )

        state.snake.forEach {
            Box(
                modifier = Modifier
                    .offset(x = tileSize * it.first, y = tileSize * it.second)
                    .size(tileSize)
                    .background(
                        DarkGreen, Shapes.small
                    )
            )
        }
    }
}