package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.offline.LocalOffline
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState

val LocalSlowLoadingHint = staticCompositionLocalOf<String?> { null }

@Composable
fun <T> UiStateHost(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "As informações aparecerão aqui quando estiverem disponíveis.",
    unavailableMessage: String? = null,
    loading: @Composable () -> Unit = { ContentSkeleton(modifier) },
    onBack: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    inline: Boolean = false,
    empty: (@Composable () -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    if (state !is UiState.Content && (header != null || inline || state == UiState.Empty || (state is UiState.Error && state.kind == ErrorKind.NOT_SUPPORTED))) {
        val noticeContent: @Composable () -> Unit = {
            when (state) {
                UiState.Loading -> loading()
                UiState.Empty -> if (empty != null) empty() else StateNotice(
                    "Nada por aqui ainda", emptyMessage,
                    actions = if (!inline && onBack != null) { { PrimaryButton("Voltar", onBack, secondary = true, compact = true) } } else null,
                )
                is UiState.Error -> {
                    val message = state.kind.presentation()
                    val offline = LocalOffline.current
                    StateNotice(
                        title = if (offline && state.kind == ErrorKind.CONNECTIVITY) "Conteúdo indisponível offline" else message.title,
                        description = when {
                            offline && state.kind == ErrorKind.CONNECTIVITY -> "Estas informações ainda não estão disponíveis para leitura offline. Reconecte-se para carregá-las."
                            state.kind == ErrorKind.NOT_SUPPORTED -> unavailableMessage ?: message.description
                            else -> message.description
                        },
                        actions = when {
                            message.canRetry -> { { PrimaryButton("Tentar novamente", onRetry, enabled = !offline, compact = true) } }
                            !inline && onBack != null -> { { PrimaryButton("Voltar", onBack, secondary = true, compact = true) } }
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
                actionLabel = if (message.canRetry) "Tentar novamente" else null,
                onAction = if (message.canRetry) onRetry else null, onBack = onBack)
        }
    }
}

fun ErrorKind.userMessage(): String = when (this) {
    ErrorKind.CONNECTIVITY -> "Sem conexão. Verifique sua internet."
    ErrorKind.TIMEOUT -> "O servidor demorou para responder."
    ErrorKind.UNAUTHORIZED -> "Sua sessão expirou. Entre novamente."
    ErrorKind.FORBIDDEN -> "Você não tem acesso a este conteúdo."
    ErrorKind.NOT_FOUND -> "Conteúdo não encontrado."
    ErrorKind.CONFLICT -> "Houve um conflito ao salvar."
    ErrorKind.VALIDATION -> "Alguns dados são inválidos."
    ErrorKind.RATE_LIMITED -> "Muitas tentativas. Tente de novo em instantes."
    ErrorKind.SERVER -> "O servidor está com problemas."
    ErrorKind.SERIALIZATION -> "Resposta inesperada do servidor."
    ErrorKind.NOT_SUPPORTED -> "Este recurso ainda não está integrado ao aplicativo."
    ErrorKind.UNKNOWN -> "Algo deu errado."
}
