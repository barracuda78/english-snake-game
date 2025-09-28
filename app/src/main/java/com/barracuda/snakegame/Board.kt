package com.barracuda.snakegame

import androidx.compose.foundation.BorderStroke // Import BorderStroke
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
// import com.barracuda.snakegame.ui.theme.DarkGreen // No longer using DarkGreen from theme for border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color // Import for custom color
import com.barracuda.snakegame.ui.theme.Shapes

@Composable
fun Board(state: State) {
    BoxWithConstraints(Modifier.padding(2.dp)) { // This outer padding is for the component itself
        val borderThickness = 2.dp
        val innerContentPadding = 1.dp // New visual gap inside the border

        // Total available width/height for the bordered box and its content
        val totalAvailableWidth = maxWidth
        // val totalAvailableHeight = maxHeight // Assuming square board for now

        // The area where game elements will be drawn, inside border and inner padding
        val gameContentAreaWidth = totalAvailableWidth - (borderThickness * 2) - (innerContentPadding * 2)
        // val gameContentAreaHeight = totalAvailableHeight - (borderThickness * 2) - (innerContentPadding * 2)

        val tileSize = gameContentAreaWidth / Game.BOARD_SIZE // Tile size based on the actual game content area

        // The Box that draws the border. It uses the full available width.
        Box(
            Modifier
                .size(totalAvailableWidth) // The bordered box takes full available space
                .border(
                    BorderStroke( // Correctly use BorderStroke here
                        borderThickness,
                        Brush.linearGradient(
                            colors = listOf(
//                                Color(0xFF00ffee),
//                                Color(0xFF00e5ff),
//                                Color(0xFF00ffee)
                                Color(0xFF557eaa),
                                Color(0xFF337dcc),
                                Color(0xFF557eaa)
                            )
                        )
                    )
                    // If you want a specific shape for the border, like rounded corners:
                    // , shape = Shapes.medium // or RoundedCornerShape(desiredRadius)
                ) 
        )

        // Offset for game elements to start drawing *after* the border and *after* innerContentPadding
        val contentStartX = borderThickness + innerContentPadding
        val contentStartY = borderThickness + innerContentPadding // Assuming square, for Y as well

        // Display the target food letter
        Text(
            text = state.targetLetter.uppercase(),
            modifier = Modifier
                .offset(x = contentStartX + (tileSize * state.targetLetterPosition.first), y = contentStartY + (tileSize * state.targetLetterPosition.second))
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
                .offset(x = contentStartX + (tileSize * state.distractorLetterPosition.first), y = contentStartY + (tileSize * state.distractorLetterPosition.second))
                .size(tileSize),
            color = state.distractorLetterColor,
            fontSize = (tileSize.value * 0.9).sp, // Make font size relative to tile size
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        state.snake.forEach { segment -> // 'it' is now a SnakeSegment
            Box(
                modifier = Modifier
                    .offset(x = contentStartX + (tileSize * segment.position.first), y = contentStartY + (tileSize * segment.position.second))
                    .size(tileSize)
                    .background(
                        color = segment.color, // Use the segment's specific color
                        shape = Shapes.small
                    )
            )
        }
    }
}