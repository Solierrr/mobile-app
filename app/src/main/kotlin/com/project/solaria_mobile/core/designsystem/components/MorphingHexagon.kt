package com.project.solaria_mobile.core.designsystem.components

import android.provider.Settings
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

private const val CYCLE_MILLIS_PER_SHAPE = 1400

@Composable
fun MorphingHexagon(
    modifier: Modifier = Modifier,
) {
    val morphs = remember {
        val shapes = listOf(
            MorphShapes.hexagon,
            MorphShapes.clover,
            MorphShapes.cookie,
            MorphShapes.pill,
            MorphShapes.pentagon,
            MorphShapes.burst,
            MorphShapes.sun,
        )
        shapes.indices.map { Morph(shapes[it], shapes[(it + 1) % shapes.size]) }
    }
    val animationsEnabled = animationsEnabled()
    val position = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "hexagon-morph")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = morphs.size.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = CYCLE_MILLIS_PER_SHAPE * morphs.size, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "hexagon-position",
        )
        animated
    } else {
        0f
    }
    val index = position.toInt().coerceIn(0, morphs.size - 1)
    val progress = EaseInOutCubic.transform((position - index).coerceIn(0f, 1f))

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val path = morphs[index].toPath(progress).asComposePath()
        // Cada forma tem um centro diferente; centralizar o contorno mantém o conteúdo (as mãos) no meio.
        val bounds = path.getBounds()
        path.translate(Offset(CENTER - bounds.center.x, CENTER - bounds.center.y))
        path.transform(Matrix().apply { scale(size.width, size.height) })
        drawBrandShape(path)
    }
}

private const val CENTER = 0.5f
private val BRAND_TOP = Color(0xFFF57D2D)
private val BRAND_BOTTOM = Color(0xFFE85900)

/** Degradê, sombra e brilho do hexágono da marca, os mesmos do web (MorphingHexagon.tsx). */
private fun DrawScope.drawBrandShape(path: Path) {
    val unit = size.minDimension
    val shadowShift = unit * 0.035f
    listOf(0.045f to 0.025f, 0.03f to 0.03f, 0.015f to 0.04f).forEach { (grow, alpha) ->
        val shadow = Path().apply { addPath(path) }
        shadow.translate(Offset(0f, shadowShift))
        drawPath(shadow, Color.Black.copy(alpha = alpha), style = Stroke(width = unit * grow * 2))
    }
    drawPath(
        path,
        Brush.linearGradient(
            colors = listOf(BRAND_TOP, BRAND_BOTTOM),
            start = Offset(size.width / 2, size.height * 0.08f),
            end = Offset(size.width / 2, size.height * 0.92f),
        ),
    )
    clipPath(path) {
        listOf(0.07f to 0.14f, 0.05f to 0.16f, 0.03f to 0.2f).forEach { (width, alpha) ->
            drawPath(path, Color.White.copy(alpha = alpha), style = Stroke(width = unit * width * 2))
        }
    }
}

@Composable
private fun animationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}
