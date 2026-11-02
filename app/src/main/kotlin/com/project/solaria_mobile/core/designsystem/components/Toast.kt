package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Motion
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Natureza do aviso: define o ícone, a cor do selo e quanto tempo o cartão fica na tela. */
enum class ToastKind(@DrawableRes val icon: Int, val millis: Long) {
    Info(AppIcons.info, 3_500),
    Success(AppIcons.check, 3_000),
    Error(AppIcons.shield_alert, 5_000),
}

data class ToastMessage(val text: String, val kind: ToastKind)

/**
 * Fila de avisos de uma tela. Cada aviso aparece, some sozinho depois de alguns segundos mesmo sem toque e dá a vez ao
 * próximo; tocar no cartão fecha antes. [show] só retorna quando o aviso sai da tela.
 */
@Stable
class ToastState {
    var current by mutableStateOf<ToastMessage?>(null)
        private set
    private val queue = Mutex()
    private val dismissals = Channel<Unit>(Channel.CONFLATED)

    suspend fun show(message: String, kind: ToastKind = ToastKind.Info) {
        queue.withLock {
            dismissals.tryReceive()
            current = ToastMessage(message, kind)
            try {
                withTimeoutOrNull(kind.millis) { dismissals.receive() }
            } finally {
                current = null
            }
        }
    }

    fun dismiss() {
        dismissals.trySend(Unit)
    }
}

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

/**
 * Onde os avisos aparecem. Posicione na base da tela, por exemplo
 * `AppToastHost(state, Modifier.align(Alignment.BottomCenter).padding(Spacing.md))`.
 */
@Composable
fun AppToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val current = state.current
    var last by remember { mutableStateOf<ToastMessage?>(null) }
    SideEffect { if (current != null) last = current }
    val shown = current ?: last
    AnimatedVisibility(
        visible = current != null,
        modifier = modifier,
        enter = slideInVertically(tween(Motion.transitionMillis * 2)) { it } + fadeIn(tween(Motion.transitionMillis)),
        exit = slideOutVertically(tween(Motion.transitionMillis)) { it / 2 } + fadeOut(tween(Motion.transitionMillis)),
    ) {
        if (shown != null) ToastCard(shown, state::dismiss)
    }
}

/** Cartão escuro com um selo colorido pelo tipo do aviso e o texto à direita; tocar nele fecha. */
@Composable
private fun ToastCard(message: ToastMessage, onDismiss: () -> Unit) {
    val badge = when (message.kind) {
        ToastKind.Success -> AppTheme.colors.success
        ToastKind.Error -> AppTheme.colors.danger
        ToastKind.Info -> Color.White.copy(alpha = 0.18f)
    }
    Surface(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(Radius.large),
        color = AppTheme.colors.onSurface,
        shadowElevation = TOAST_ELEVATION,
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(TOAST_BADGE).clip(CircleShape).background(badge), contentAlignment = Alignment.Center) {
                Icon(painterResource(message.kind.icon), null, Modifier.size(TOAST_ICON), tint = AppTheme.colors.onAccent)
            }
            Text(
                message.text,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.onAccent,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val TOAST_ELEVATION = 6.dp
private val TOAST_BADGE = 32.dp
private val TOAST_ICON = 18.dp

/** Mostra [message] uma vez e avisa por [onDismiss] quando ele sai da tela, para o estado de origem esquecer a mensagem. */
@Composable
fun TransientFeedback(message: String?, onDismiss: (String) -> Unit, modifier: Modifier = Modifier, kind: ToastKind = ToastKind.Success) {
    val toast = rememberToastState()
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(message) {
        if (message != null) {
            toast.show(message, kind)
            dismiss(message)
        }
    }
    AppToastHost(toast, modifier)
}
