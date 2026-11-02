package com.project.solaria_mobile.core.designsystem.components

import com.project.solaria_mobile.core.designsystem.theme.Sizing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.ProvideSurfaceContainers
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import kotlin.math.max
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Tamanhos das sheets. Compacto acompanha o conteúdo, para menus curtos; conteúdo cresce até o limite e rola;
 * expandido ocupa a altura útil desde o início, para listas longas como a galeria.
 */
enum class SheetSize { Compact, Content, Expanded }

private const val CONTENT_MAX_FRACTION = 0.85f
private const val EXPANDED_FRACTION = 0.9f
private const val SCRIM_ALPHA = 0.28f
private const val DISMISS_VELOCITY = 800f
private const val DISMISS_FRACTION = 0.15f

// A entrada usa uma mola com leve excesso (elástica); a saída é uma curva curta e firme, sem quicar.
private val ShowSpec = spring<Float>(dampingRatio = 0.78f, stiffness = 320f)
private val HideSpec = tween<Float>(durationMillis = 240, easing = FastOutLinearInEasing)

// O pino excede o repouso durante o quique; esta faixa extra por baixo evita mostrar um vão sob a sheet.
private val Overshoot = 56.dp

/** Estado de uma sheet: [hidden] vai de 0 (aberta) a 1 (fora da tela) e pode passar de 0 durante o quique. */
@Stable
class SheetState internal constructor(private val confirmDismiss: () -> Boolean) {
    internal val hidden = Animatable(1f)
    internal var height by mutableFloatStateOf(0f)
    val isVisible: Boolean get() = hidden.value < 1f

    internal suspend fun show() { hidden.animateTo(0f, ShowSpec) }

    /** Anima a saída e só retorna quando a sheet já saiu da tela. */
    suspend fun hide() { hidden.animateTo(1f, HideSpec) }

    internal fun confirm(): Boolean = confirmDismiss()
}

/** [confirmDismiss] pode recusar o fechamento (a sheet volta para o lugar), por exemplo para voltar um nível antes. */
@Composable
fun rememberSheetState(confirmDismiss: () -> Boolean = { true }): SheetState {
    val latest by rememberUpdatedState(confirmDismiss)
    return remember { SheetState { latest() } }
}

/** Fecha a sheet atual com a animação de saída; o conteúdo pode chamar isto em vez de remover a sheet de uma vez. */
val LocalSheetDismiss = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun BottomSheet(
    onDismiss: () -> Unit,
    state: SheetState = rememberSheetState(),
    scrollable: Boolean = true,
    size: SheetSize = SheetSize.Content,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var closing by remember { mutableFloatStateOf(0f) }
    val dismiss: () -> Unit = {
        if (closing == 0f) scope.launch {
            if (state.confirm()) {
                closing = 1f
                state.hide()
                currentOnDismiss()
                closing = 0f
            } else state.hidden.animateTo(0f, ShowSpec)
        }
    }
    // A entrada só começa depois de medir a altura; assim a mola não gasta quadros com a sheet ainda invisível.
    LaunchedEffect(state, state.height > 0f) { if (state.height > 0f) state.show() }

    // Só o puxador empurra a sheet; o conteúdo nunca a move. Ela acompanha o dedo e, se for solta depois de descer
    // um pouco (ou num gesto rápido), desce inteira; soltando antes, o usuário desistiu e ela volta com a mola.
    fun drag(deltaPx: Float) {
        if (state.height <= 0f) return
        val next = max(0f, state.hidden.value + deltaPx / state.height)
        scope.launch(start = CoroutineStart.UNDISPATCHED) { state.hidden.snapTo(next) }
    }
    fun settle(velocityPx: Float) {
        if (state.hidden.value > DISMISS_FRACTION || velocityPx > DISMISS_VELOCITY) dismiss()
        else scope.launch { state.hidden.animateTo(0f, ShowSpec, initialVelocity = velocityPx / max(state.height, 1f)) }
    }
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val heightModifier = when (size) {
        SheetSize.Compact -> Modifier
        SheetSize.Content -> Modifier.heightIn(max = (screenHeight * CONTENT_MAX_FRACTION).dp)
        SheetSize.Expanded -> Modifier.height((screenHeight * EXPANDED_FRACTION).dp)
    }
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        val overshootPx = with(androidx.compose.ui.platform.LocalDensity.current) { Overshoot.toPx() }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { alpha = (1f - state.hidden.value).coerceIn(0f, 1f) }
                    .background(AppTheme.colors.onSurface.copy(alpha = SCRIM_ALPHA))
                    .pointerInput(Unit) { detectTapGestures { dismiss() } },
            )
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding()
                    .onSizeChanged { state.height = it.height.toFloat() }
                    .graphicsLayer {
                        // Até medir a altura, mantém a sheet fora da tela para não piscar no lugar final.
                        alpha = if (state.height > 0f) 1f else 0f
                        translationY = state.hidden.value * state.height + overshootPx
                    }
                    .clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                    .background(AppTheme.colors.surface),
            ) {
                Box(
                    Modifier.fillMaxWidth()
                        .draggable(rememberDraggableState { drag(it) }, Orientation.Vertical, onDragStopped = { settle(it) })
                        .padding(vertical = Spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.width(Sizing.sheetHandleWidth).height(Sizing.sheetHandleHeight)
                            .clip(RoundedCornerShape(Radius.pill)).background(AppTheme.colors.onSurfaceSubtle),
                    )
                }
                CompositionLocalProvider(LocalSheetDismiss provides dismiss) {
                    ProvideSurfaceContainers {
                        Column(
                            Modifier.fillMaxWidth().then(heightModifier)
                                .navigationBarsPadding().then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier),
                            content = content,
                        )
                    }
                }
                Spacer(Modifier.height(Overshoot))
            }
        }
    }
}
