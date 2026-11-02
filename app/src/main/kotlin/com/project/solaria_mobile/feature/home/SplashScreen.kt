package com.project.solaria_mobile.feature.home

import android.graphics.Point
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.solaria_mobile.core.designsystem.components.BrandLoader
import com.project.solaria_mobile.core.designsystem.components.MorphShapes
import com.project.solaria_mobile.core.designsystem.components.SPLASH_BRAND_SIZE
import com.project.solaria_mobile.core.designsystem.components.animationsEnabled
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot

/** Ocupa a janela inteira, atrás das barras do sistema, para a marca ficar no mesmo ponto da splash nativa. */
@Composable
fun SplashScreen() {
    val view = LocalView.current
    // Janela inteira, atrás das barras. Fica em estado porque na primeira medição, durante a splash nativa, o tamanho ainda é zero.
    var window by remember { mutableStateOf(fullWindowSize(view)) }
    var origin by remember {
        val bars = ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.systemBars())
        mutableStateOf(IntOffset(bars?.left ?: 0, bars?.top ?: 0))
    }
    val density = LocalDensity.current
    val badgeScale = remember { Animatable(if (SplashTiming.played) 1f else 0f) }
    LaunchedEffect(Unit) { badgeScale.animateTo(1f, tween(BADGE_EXPAND_MILLIS, easing = EaseOutCubic)) }
    Box(
        Modifier.fillMaxSize().onGloballyPositioned {
            origin = it.positionInWindow().round()
            window = fullWindowSize(view)
        },
    ) {
        // Bloco do tamanho da tela inteira, sem se limitar ao espaço que o pai oferece, puxado de volta para o canto da tela.
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset { IntOffset(-origin.x, -origin.y) }
                .requiredSize(with(density) { window.width.toDp() }, with(density) { window.height.toDp() }),
            contentAlignment = Alignment.Center,
        ) {
            SplashGrid(animated = animationsEnabled())
            // Um círculo branco separa a logo do laranja do fundo, que tem tom diferente do dela; cresce do centro para fora.
            Box(Modifier.size(SPLASH_BRAND_SIZE * BADGE_SCALE).graphicsLayer { scaleX = badgeScale.value; scaleY = badgeScale.value }.background(Color.White, CircleShape))
            // O hexágono da marca fica sempre igual e só gira, como nas telas de carregamento do web.
            BrandLoader(Modifier.size(SPLASH_BRAND_SIZE), rotateShape = true, millisPerShape = SPIN_MILLIS, shapes = listOf(MorphShapes.hexagon))
        }
    }
}

/** Tela inteira, com as barras do sistema: é nela que a splash nativa centraliza a marca, mesmo com a janela do app mais curta. */
private fun fullWindowSize(view: View): IntSize {
    val windows = view.context.getSystemService(WindowManager::class.java)
    val size = Point()
    @Suppress("DEPRECATION")
    windows.defaultDisplay.getRealSize(size)
    return IntSize(size.x, size.y)
}

/** Tempos da splash. A abertura a frio mostra a introdução inteira uma vez por processo, mesmo com a sessão já pronta. */
object SplashTiming {
    const val INTRO_MILLIS = 2400
    const val REDUCED_MILLIS = 600

    /** Fica verdadeiro depois da primeira introdução; voltar à splash (sair da conta, entrar de novo) não a repete. */
    var played = false
}

/** Uma volta completa da logo, o tamanho do círculo branco em relação a ela e o tempo que ele leva para crescer. */
private const val SPIN_MILLIS = 6000
private const val BADGE_SCALE = 1.3f
private const val BADGE_EXPAND_MILLIS = 700

/** Laranja da marca; é também a cor de fundo da splash nativa (themes.xml). */
private val BRAND_ORANGE = Color(0xFFFF6500)
private val BRAND_ORANGE_LIGHT = Color(0xFFFF7A1F)

private const val PULSE_SECONDS = 2.6f
private const val CELL_DP = 40f
private const val CELL_INSET_DP = 4f
private const val CELL_RADIUS_DP = 5f
private const val LINE_ALPHA = 0.16f
private const val DOT_ALPHA = 0.3f
private const val WAVE_PEAK = 0.3f
private const val WAVE_MAX_ALPHA = 0.45f
private const val WAVE_REACH = 1.15f
private const val WAVE_CROSSING = 0.9f
private const val WAVE_SPREAD_CELLS = 1.1f
private const val GRID_FADE_IN_MILLIS = 500
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val VISIBLE_ALPHA = 0.012f

/**
 * Fundo da splash: laranja da marca com a grade de quadrados do login do web. O laranja respira e, a cada pulso, uma
 * onda de células acesas sai do centro (onde fica a logo) e se dissipa. A grade aparece com um fade logo depois da
 * splash nativa, que só tem o laranja. Sem animação no aparelho, fica parado.
 */
@Composable
private fun SplashGrid(animated: Boolean) {
    val time = remember { mutableFloatStateOf(0f) }
    val appear = remember { Animatable(if (SplashTiming.played) 1f else 0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(GRID_FADE_IN_MILLIS)) }
    LaunchedEffect(animated) {
        if (!animated) return@LaunchedEffect
        var start = 0L
        while (true) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                time.floatValue = (now - start) / NANOS_PER_SECOND
            }
        }
    }
    val cell = with(LocalDensity.current) { CELL_DP.dp.toPx() }
    Canvas(Modifier.fillMaxSize()) {
        val t = time.floatValue
        val visible = appear.value
        val pulse = 0.5f - 0.5f * cos(2f * PI.toFloat() * t / PULSE_SECONDS)
        drawRect(lerp(BRAND_ORANGE, BRAND_ORANGE_LIGHT, pulse))

        val columns = ceil(size.width / cell).toInt()
        val rows = ceil(size.height / cell).toInt()
        val offsetX = (size.width - columns * cell) / 2f
        val offsetY = (size.height - rows * cell) / 2f
        val line = Color.White.copy(alpha = LINE_ALPHA * visible)
        for (column in 0..columns) {
            val x = offsetX + column * cell
            drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        }
        for (row in 0..rows) {
            val y = offsetY + row * cell
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val dot = Color.White.copy(alpha = DOT_ALPHA * visible)
        for (row in 0..rows) {
            for (column in 0..columns) {
                drawRect(dot, Offset(offsetX + column * cell - 1f, offsetY + row * cell - 1f), Size(2f, 2f))
            }
        }

        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val speed = hypot(centerX, centerY) / (PULSE_SECONDS * WAVE_CROSSING)
        val spread = cell * WAVE_SPREAD_CELLS
        val life = PULSE_SECONDS * WAVE_REACH
        val age = t % PULSE_SECONDS
        val inset = CELL_INSET_DP * density
        val radius = CornerRadius(CELL_RADIUS_DP * density)
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val left = offsetX + column * cell
                val top = offsetY + row * cell
                val distance = hypot(left + cell / 2f - centerX, top + cell / 2f - centerY)
                var alpha = 0f
                for (wave in 0..1) {
                    val waveAge = age + wave * PULSE_SECONDS
                    if (waveAge > life) continue
                    val away = (distance - waveAge * speed) / spread
                    alpha += WAVE_PEAK * (1f - waveAge / life) * exp(-away * away)
                }
                alpha = alpha.coerceAtMost(WAVE_MAX_ALPHA) * visible
                if (alpha < VISIBLE_ALPHA) continue
                drawRoundRect(
                    Color.White.copy(alpha = alpha),
                    Offset(left + inset, top + inset),
                    Size(cell - 2f * inset, cell - 2f * inset),
                    radius,
                )
            }
        }
    }
}
