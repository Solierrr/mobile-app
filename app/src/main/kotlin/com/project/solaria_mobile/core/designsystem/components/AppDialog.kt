package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing

/**
 * Popup padrão do app: título, mensagem opcional (ou conteúdo próprio) e dois botões do mesmo tamanho, que juntos
 * ocupam toda a largura. Cancelar fica à esquerda; confirmar à direita, destrutivo em vermelho quando [destructive].
 * Com [loading], o confirmar mostra o indicador e o cancelar fica desabilitado. Sem [dismissLabel], só o confirmar
 * aparece, como num aviso com um único OK.
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    title: String? = null,
    message: String? = null,
    dismissLabel: String? = "Cancelar",
    onDismiss: () -> Unit = onDismissRequest,
    destructive: Boolean = false,
    loading: Boolean = false,
    confirmEnabled: Boolean = true,
    wideContent: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = { if (!loading) onDismissRequest() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val visible = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 500f), initialScale = 0.9f),
        ) {
            Surface(
                modifier = Modifier.padding(horizontal = if (wideContent) Spacing.md else Spacing.xl).widthIn(max = MAX_WIDTH).fillMaxWidth(),
                shape = RoundedCornerShape(Radius.large),
                color = AppTheme.colors.surface,
            ) {
                Column(Modifier.padding(vertical = Spacing.section), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    val side = Modifier.padding(horizontal = Spacing.section)
                    if (title != null) Text(title, modifier = side, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.headlineSmall)
                    if (message != null) Text(message, modifier = side, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyLarge)
                    // Conteúdo largo, como os seletores de data, ocupa a largura toda; o resto respeita o padding do popup.
                    if (content != null) Column(if (wideContent) Modifier.fillMaxWidth() else side, verticalArrangement = Arrangement.spacedBy(Spacing.md)) { content() }
                    Row(side.fillMaxWidth().padding(top = Spacing.xs).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        if (dismissLabel != null) {
                            AppButton(dismissLabel, onDismiss, Modifier.weight(1f).fillMaxHeight(), ButtonVariant.Secondary, ButtonSize.Medium, enabled = !loading, fillWidth = false)
                        }
                        AppButton(
                            confirmLabel, onConfirm, Modifier.weight(1f).fillMaxHeight(),
                            if (destructive) ButtonVariant.Destructive else ButtonVariant.Primary, ButtonSize.Medium,
                            enabled = confirmEnabled, loading = loading, fillWidth = false,
                        )
                    }
                }
            }
        }
    }
}

private val MAX_WIDTH = 420.dp
