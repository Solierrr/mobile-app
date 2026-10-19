package com.project.solaria_mobile.core.designsystem.components

import android.graphics.BlurMaskFilter
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

private const val CYCLE_MILLIS_PER_SHAPE = 1400

private val shapes = listOf(
    MorphShapes.hexagon,
    MorphShapes.clover,
    MorphShapes.cookie,
    MorphShapes.pill,
    MorphShapes.pentagon,
    MorphShapes.burst,
    MorphShapes.sun,
)

@Composable
fun MorphingHexagon(
    modifier: Modifier = Modifier,
) {
    val morphs = remember {
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
        // O centro de massa muda de forma para forma; ancorá-lo no meio mantém as mãos centralizadas.
        val from = shapes[index]
        val to = shapes[(index + 1) % shapes.size]
        val centerX = from.centerX + (to.centerX - from.centerX) * progress
        val centerY = from.centerY + (to.centerY - from.centerY) * progress
        path.translate(Offset(-centerX, -centerY))
        path.transform(
            Matrix().apply {
                scale(size.width * SHAPE_SCALE, size.height * SHAPE_SCALE)
                translate(CENTER / SHAPE_SCALE, CENTER / SHAPE_SCALE)
            },
        )
        drawBrandShape(path)
    }
}

private const val CENTER = 0.5f
private const val SHAPE_SCALE = 0.88f
private val BRAND_TOP = Color(0xFFF57D2D)
private val BRAND_BOTTOM = Color(0xFFE85900)

/** Degradê vertical da marca (como no web) com uma sombra bem difusa, sem contornos. */
private fun DrawScope.drawBrandShape(path: Path) {
    val unit = size.minDimension
    drawIntoCanvas { canvas ->
        val shadow = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(SHADOW_ALPHA, 0, 0, 0)
            maskFilter = BlurMaskFilter(unit * SHADOW_BLUR, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.save()
        canvas.translate(0f, unit * SHADOW_SHIFT)
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), shadow)
        canvas.restore()
    }
    drawPath(
        path,
        Brush.linearGradient(
            colors = listOf(BRAND_TOP, BRAND_BOTTOM),
            start = Offset(size.width / 2, size.height * 0.08f),
            end = Offset(size.width / 2, size.height * 0.92f),
        ),
    )
}

private const val SHADOW_ALPHA = 36
private const val SHADOW_BLUR = 0.03f
private const val SHADOW_SHIFT = 0.02f

@Composable
private fun animationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}
