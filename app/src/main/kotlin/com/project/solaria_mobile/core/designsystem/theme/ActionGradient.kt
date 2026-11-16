package com.project.solaria_mobile.core.designsystem.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.actionGradient(): Modifier = drawWithCache {
    val length = size.width * 0.05233596f + size.height * 0.9986295f
    val x = length * 0.02616798f
    val y = length * 0.49931475f
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFFFF9B52), Color(0xFFFF650A)),
        start = Offset(size.width / 2 - x, size.height / 2 - y),
        end = Offset(size.width / 2 + x, size.height / 2 + y),
    )
    onDrawBehind { drawRect(brush) }
}
