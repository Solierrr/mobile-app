package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.AppTheme

@Composable
fun MapBackdrop(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Canvas(modifier = modifier.background(colors.headerFill)) {
        val w = size.width
        val h = size.height
        val roads = listOf(
            Path().apply { moveTo(-w * 0.1f, h * 0.22f); cubicTo(w * 0.2f, h * 0.35f, w * 0.28f, h * 0.08f, w * 0.54f, h * 0.25f); cubicTo(w * 0.77f, h * 0.39f, w * 0.82f, h * 0.12f, w * 1.1f, h * 0.26f) },
            Path().apply { moveTo(-w * 0.08f, h * 0.58f); cubicTo(w * 0.22f, h * 0.45f, w * 0.32f, h * 0.72f, w * 0.62f, h * 0.55f); cubicTo(w * 0.8f, h * 0.45f, w * 0.84f, h * 0.65f, w * 1.08f, h * 0.51f) },
            Path().apply { moveTo(w * 0.12f, -h * 0.1f); cubicTo(w * 0.32f, h * 0.2f, w * 0.06f, h * 0.52f, w * 0.32f, h * 0.75f); cubicTo(w * 0.47f, h * 0.9f, w * 0.35f, h * 1.05f, w * 0.57f, h * 1.1f) },
            Path().apply { moveTo(w * 0.78f, -h * 0.1f); cubicTo(w * 0.62f, h * 0.18f, w * 0.91f, h * 0.33f, w * 0.7f, h * 0.62f); cubicTo(w * 0.62f, h * 0.78f, w * 0.82f, h * 0.9f, w * 0.71f, h * 1.1f) },
        )
        roads.forEachIndexed { index, path ->
            drawPath(path, color = colors.surface, style = Stroke(width = if (index < 2) 5.dp.toPx() else 3.dp.toPx()))
        }
        val mainRoad = Path().apply {
            moveTo(-w * 0.05f, h * 0.82f)
            cubicTo(w * 0.24f, h * 0.63f, w * 0.17f, h * 0.48f, w * 0.44f, h * 0.43f)
            cubicTo(w * 0.73f, h * 0.38f, w * 0.72f, h * 0.21f, w * 1.05f, h * 0.08f)
        }
        drawPath(mainRoad, color = colors.mapRoad, style = Stroke(width = 13.dp.toPx()))
        val water = Path().apply {
            moveTo(-w * 0.05f, h * 0.47f)
            cubicTo(w * 0.2f, h * 0.43f, w * 0.23f, h * 0.27f, w * 0.5f, h * 0.3f)
            cubicTo(w * 0.75f, h * 0.34f, w * 0.73f, h * 0.51f, w * 1.06f, h * 0.42f)
        }
        drawPath(water, color = colors.mapWater, style = Stroke(width = 6.dp.toPx()))
        drawCircle(color = colors.mapPark, radius = w * 0.15f, center = androidx.compose.ui.geometry.Offset(w * 0.92f, h * 0.87f))
    }
}
