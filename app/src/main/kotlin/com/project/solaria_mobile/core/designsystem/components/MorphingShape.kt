package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

private const val MILLIS_PER_SHAPE = 4000

// Só formas arredondadas e de proporção quadrada: a foto continua legível em todas elas.
private val avatarShapes = listOf(
    MorphShapes.cookie,
    MorphShapes.clover,
    MorphShapes.burst,
    MorphShapes.hexagon,
    MorphShapes.sun,
    MorphShapes.pentagon,
)

private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress).asComposePath()
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

/**
 * Posição contínua da animação entre [count] formas: a parte inteira é a forma de partida e a fração, o avanço até a próxima.
 * Com as animações do aparelho desligadas, fica em zero.
 */
@Composable
internal fun rememberMorphPosition(count: Int, millisPerShape: Int, label: String): Float {
    if (!animationsEnabled()) return 0f
    val transition = rememberInfiniteTransition(label = label)
    val animated by transition.animateFloat(
        initialValue = 0f,
        targetValue = count.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = millisPerShape * count, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "$label-position",
    )
    return animated
}

/**
 * Entrega ao [content] uma forma que muda aos poucos de uma forma expressiva para a próxima, sem parar. Só este trecho
 * é recomposto a cada quadro, então a tela em volta não é afetada. Com as animações do aparelho desligadas, a forma
 * fica parada na primeira.
 */
@Composable
fun MorphingShapeHost(
    shapes: List<RoundedPolygon> = avatarShapes,
    millisPerShape: Int = MILLIS_PER_SHAPE,
    content: @Composable (Shape) -> Unit,
) {
    val morphs = remember(shapes) { shapes.indices.map { Morph(shapes[it], shapes[(it + 1) % shapes.size]) } }
    val position = rememberMorphPosition(morphs.size, millisPerShape, "shape-morph")
    val index = position.toInt().coerceIn(0, morphs.size - 1)
    val progress = EaseInOutCubic.transform((position - index).coerceIn(0f, 1f))
    content(MorphShape(morphs[index], progress))
}
