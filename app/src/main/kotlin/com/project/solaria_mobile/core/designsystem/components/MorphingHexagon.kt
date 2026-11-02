package com.project.solaria_mobile.core.designsystem.components

import android.graphics.BlurMaskFilter
import android.provider.Settings
import androidx.compose.animation.core.EaseInOutCubic
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

private const val CYCLE_MILLIS_PER_SHAPE = 1400

/** Parte do quadrado da marca que a forma ocupa (a maior dimensão). */
internal const val BRAND_SHAPE_SCALE = 0.9f

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
    rotateShape: Boolean = false,
    millisPerShape: Int = CYCLE_MILLIS_PER_SHAPE,
) {
    val morphs = remember {
        shapes.indices.map { Morph(shapes[it], shapes[(it + 1) % shapes.size]) }
    }
    val position = rememberMorphPosition(morphs.size, millisPerShape, "hexagon-morph")
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
        rotate(if (rotateShape) position / morphs.size * 360f else 0f) {
            drawBrandShape(path)
        }
    }
}

private const val CENTER = 0.5f
private const val SHAPE_SCALE = BRAND_SHAPE_SCALE
private val BRAND_TOP = Color(0xFFF57D2D)
private val BRAND_BOTTOM = Color(0xFFE85900)
private val RADIAL_CENTER = Color(0xFFFF5900)
private val RADIAL_EDGE = Color(0xFFFFAC65)

/**
 * Marca como no Figma: sombra suave, degradê vertical laranja, duas camadas leves por cima (radial e diagonal) e um
 * brilho branco interno que deixa a borda clara e o centro mais escuro. As medidas seguem a altura da forma.
 */
private fun DrawScope.drawBrandShape(path: Path) {
    val bounds = path.getBounds()
    val height = bounds.height
    if (height <= 0f) return
    drawIntoCanvas { canvas ->
        val shadow = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(SHADOW_ALPHA, 0, 0, 0)
            maskFilter = BlurMaskFilter(height * SHADOW_BLUR, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.save()
        canvas.translate(0f, height * SHADOW_SHIFT)
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), shadow)
        canvas.restore()
    }
    drawPath(
        path,
        Brush.verticalGradient(
            colors = listOf(BRAND_TOP, BRAND_BOTTOM),
            startY = bounds.top - height * GRADIENT_OVERSHOOT,
            endY = bounds.bottom + height * GRADIENT_OVERSHOOT,
        ),
    )
    drawPath(
        path,
        Brush.radialGradient(
            colors = listOf(RADIAL_CENTER.copy(alpha = RADIAL_ALPHA), RADIAL_EDGE.copy(alpha = RADIAL_ALPHA)),
            center = bounds.center,
            radius = height / 2f,
        ),
    )
    drawPath(
        path,
        Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = DIAGONAL_TOP_ALPHA), BRAND_BOTTOM.copy(alpha = DIAGONAL_BOTTOM_ALPHA)),
            start = bounds.topLeft,
            end = bounds.bottomRight,
        ),
    )
    clipPath(path) {
        drawIntoCanvas { canvas ->
            val glow = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = height * GLOW_INSET * 2f
                color = android.graphics.Color.argb(GLOW_ALPHA, 255, 255, 255)
                maskFilter = BlurMaskFilter(height * GLOW_BLUR, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawPath(path.asAndroidPath(), glow)
        }
    }
}

private const val SHADOW_ALPHA = 26
private const val SHADOW_BLUR = 0.063f
private const val SHADOW_SHIFT = 0.0365f
private const val GRADIENT_OVERSHOOT = 0.008f
private const val RADIAL_ALPHA = 0.05f
private const val DIAGONAL_TOP_ALPHA = 0.08f
private const val DIAGONAL_BOTTOM_ALPHA = 0.2f
private const val GLOW_INSET = 0.0307f
private const val GLOW_BLUR = 0.063f
private const val GLOW_ALPHA = 64

/** Falso quando o aparelho zerou a escala de animação; a marca então fica parada no hexágono. */
@Composable
internal fun animationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}
