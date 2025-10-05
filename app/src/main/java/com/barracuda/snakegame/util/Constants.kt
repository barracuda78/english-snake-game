package com.barracuda.snakegame.util

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object GameConstants {
    // Game Logic
    const val BOARD_SIZE = 32
    const val INITIAL_SNAKE_LENGTH = 4
    const val BASE_DELAY_MS = 200L
    const val MIN_DELAY_MS = 50L
    const val DELAY_DECREASE_PER_FOOD_MS = 5L

    // UI Dimensions
    val ButtonSize = 64.dp
    val ButtonHeight = 48.dp
    val MenuButtonWidth = 0.6f
    val PauseButtonWidth = 0.5f

    // Paddings and Spacers
    val ScreenPadding = 8.dp
    val ScorePaddingHorizontal = 16.dp
    val ScorePaddingVertical = 4.dp
    val PauseButtonPaddingTop = 16.dp
    val ButtonColumnPadding = 24.dp
    val StartMenuSpacerHeight = 64.dp
    val MenuButtonSpacerHeight = 16.dp
    val GameOverSpacerHeight1 = 16.dp
    val GameOverSpacerHeight2 = 32.dp
    val AlphabetRowPaddingVertical = 8.dp
    val AlphabetLetterSpacing = 4.dp
    val DialogConfirmButtonPaddingBottom = 8.dp
    val DialogConfirmButtonPaddingHorizontal = 8.dp

    // Elevations
    val ButtonElevationDefault = 2.dp
    val ButtonElevationPressed = 4.dp

    // Font Sizes
    val ScoreFontSize = 20.sp
    val PausedFontSize = 32.sp
    val TitleFontSize = 40.sp
    val MenuButtonFontSize = 20.sp
    val GameOverTitleFontSize = 32.sp
    val GameOverScoreFontSize = 24.sp
    val AlphabetFontSize = 14.sp

    // Shapes
    val ButtonCornerRadius = 12.dp
    val DialogCornerRadius = 16.dp
    val BoardCornerRadius = 4.dp

    // Board
    val BoardPadding = 2.dp
    val BoardBorderThickness = 1.dp
    val BoardInnerPadding = 1.dp
}