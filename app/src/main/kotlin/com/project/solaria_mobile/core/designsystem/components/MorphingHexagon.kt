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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.project.solaria_mobile.core.designsystem.theme.AppTheme

private const val CYCLE_MILLIS_PER_SHAPE = 1400

@Composable
fun MorphingHexagon(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.secondary,
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
        path.transform(Matrix().apply { scale(size.width, size.height) })
        drawPath(path, color)
    }
}

@Composable
private fun animationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}
