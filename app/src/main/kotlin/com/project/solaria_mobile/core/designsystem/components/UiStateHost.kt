package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.R
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.project.solaria_mobile.core.designsystem.theme.Motion
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.offline.LocalOffline
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState

val LocalSlowLoadingHint = staticCompositionLocalOf<String?> { null }

private enum class HostPhase { LOADING, CONTENT, NOTICE }

/**
 * Troca o esqueleto pelo conteúdo (ou pelo aviso) com um fade. A troca acompanha só a fase: atualizar o conteúdo já exibido
 * não reinicia a tela. Durante o fade, a fase que sai continua mostrando o último estado que teve.
 */
@Composable
fun <T> UiStateHost(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = stringResource(R.string.ui_as_informacoes_aparecerao_aqui_quando),
    unavailableMessage: String? = null,
    loading: @Composable () -> Unit = { ContentSkeleton() },
    onBack: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    inline: Boolean = false,
    empty: (@Composable () -> Unit)? = null,
    loadingHeader: Boolean = true,
    content: @Composable (T) -> Unit,
) {
    fun phaseOf(value: UiState<T>) = when (value) {
        is UiState.Content -> HostPhase.CONTENT
        UiState.Loading -> HostPhase.LOADING
        else -> HostPhase.NOTICE
    }
    val phase = phaseOf(state)
    val lastByPhase = remember { mutableMapOf<HostPhase, UiState<T>>() }
    lastByPhase[phase] = state
    Crossfade(phase, modifier, animationSpec = tween(Motion.transitionMillis), label = "ui-state") { shown ->
        val shownState = if (shown == phase) state else lastByPhase[shown]
        if (shownState != null) UiStateBody(shownState, onRetry, Modifier, emptyMessage, unavailableMessage, loading, onBack, header, inline, empty, loadingHeader, content)
    }
}

@Composable
private fun <T> UiStateBody(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier,
    emptyMessage: String,
    unavailableMessage: String?,
    loading: @Composable () -> Unit,
    onBack: (() -> Unit)?,
    header: (@Composable () -> Unit)?,
    inline: Boolean,
    empty: (@Composable () -> Unit)?,
    loadingHeader: Boolean,
    content: @Composable (T) -> Unit,
) {
    // O esqueleto ocupa o mesmo lugar do conteúdo: começa no topo, abaixo do cabeçalho, em vez de centralizado como os avisos.
    if (state == UiState.Loading && !inline) {
        if (header != null && loadingHeader) {
            Column(modifier.fillMaxSize().padding(horizontal = Spacing.screen).padding(top = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.section)) {
                header()
                loading()
            }
        } else loading()
        return
    }
    if (state !is UiState.Content && (header != null || inline || state == UiState.Empty || (state is UiState.Error && state.kind == ErrorKind.NOT_SUPPORTED))) {
        val noticeContent: @Composable () -> Unit = {
            when (state) {
                UiState.Loading -> loading()
                UiState.Empty -> if (empty != null) empty() else StateNotice(
                    stringResource(R.string.ui_nada_por_aqui_ainda), emptyMessage,
                    actions = if (!inline && onBack != null) { { PrimaryButton(stringResource(R.string.common_back), onBack, secondary = true, compact = true) } } else null,
                )
                is UiState.Error -> {
                    val message = state.kind.presentation()
                    val offline = LocalOffline.current
                    StateNotice(
                        title = if (offline && state.kind == ErrorKind.CONNECTIVITY) stringResource(R.string.ui_conteudo_indisponivel_offline) else message.title,
                        description = when {
                            offline && state.kind == ErrorKind.CONNECTIVITY -> stringResource(R.string.ui_estas_informacoes_ainda_nao_estao)
                            state.kind == ErrorKind.NOT_SUPPORTED -> unavailableMessage ?: message.description
                            else -> message.description
                        },
                        actions = when {
                            message.canRetry -> { { PrimaryButton(stringResource(R.string.common_try_again), onRetry, enabled = !offline, compact = true) } }
                            !inline && onBack != null -> { { PrimaryButton(stringResource(R.string.common_back), onBack, secondary = true, compact = true) } }
                            else -> null
                        },
                    )
                }
                else -> Unit
            }
        }
        if (inline) Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.section)) {
            header?.invoke()
            noticeContent()
        } else StateNoticePage(header, modifier, noticeContent)
        return
    }
    when (state) {
        is UiState.Content -> content(state.data)
        UiState.Loading -> loading()
        UiState.Empty -> Unit
        is UiState.Error -> {
            val message = state.kind.presentation()
            StateMessage(message.title, if (state.kind == ErrorKind.NOT_SUPPORTED) unavailableMessage ?: message.description else message.description, modifier, animateBrand = true,
                actionLabel = if (message.canRetry) stringResource(R.string.common_try_again) else null,
                onAction = if (message.canRetry) onRetry else null, onBack = onBack)
        }
    }
}

/** Mensagem curta do erro no idioma do aplicativo; fora de uma tela, passe os [resources] da tela atual. */
fun ErrorKind.userMessage(resources: android.content.res.Resources): String = resources.getString(
    when (this) {
        ErrorKind.CONNECTIVITY -> R.string.error_connectivity
        ErrorKind.TIMEOUT -> R.string.error_timeout
        ErrorKind.UNAUTHORIZED -> R.string.error_unauthorized
        ErrorKind.FORBIDDEN -> R.string.error_forbidden
        ErrorKind.NOT_FOUND -> R.string.error_not_found
        ErrorKind.CONFLICT -> R.string.error_conflict
        ErrorKind.VALIDATION -> R.string.error_validation
        ErrorKind.RATE_LIMITED -> R.string.error_rate_limited
        ErrorKind.SERVER -> R.string.error_server
        ErrorKind.SERIALIZATION -> R.string.error_serialization
        ErrorKind.NOT_SUPPORTED -> R.string.error_not_supported
        ErrorKind.UNKNOWN -> R.string.error_unknown
    },
)

@Composable
fun ErrorKind.userMessage(): String = userMessage(androidx.compose.ui.platform.LocalContext.current.resources)
