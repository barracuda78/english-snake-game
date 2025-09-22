package com.barracuda.snakegame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.material.Text // Added for food letter
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight // Added for bold text
import androidx.compose.ui.text.style.TextAlign // Added for text alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // For text size
import com.barracuda.snakegame.ui.theme.DarkGreen
import com.barracuda.snakegame.ui.theme.Shapes

@Composable
fun Board(state: State) {
    BoxWithConstraints(Modifier.padding(2.dp)) {
        val tileSize = maxWidth / Game.BOARD_SIZE

        Box(
            Modifier
                .size(maxWidth)
                .border(2.dp, DarkGreen)
        )

        // Display the target food letter
        Text(
            text = state.targetLetter.uppercase(),
            modifier = Modifier
                .offset(x = tileSize * state.targetLetterPosition.first, y = tileSize * state.targetLetterPosition.second)
                .size(tileSize),
            color = state.targetLetterColor,
            fontSize = (tileSize.value * 0.9).sp, // Make font size relative to tile size
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold // Added for bold text
        )

        // Display the distractor food letter
        Text(
            text = state.distractorLetter.uppercase(),
            modifier = Modifier
                .offset(x = tileSize * state.distractorLetterPosition.first, y = tileSize * state.distractorLetterPosition.second)
                .size(tileSize),
            color = state.distractorLetterColor,
            fontSize = (tileSize.value * 0.9).sp, // Make font size relative to tile size
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
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