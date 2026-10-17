package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import kotlinx.coroutines.delay

val LocalSlowLoadingHint = staticCompositionLocalOf<String?> { null }

private const val SLOW_LOADING_DELAY_MILLIS = 4_000L

@Composable
fun <T> UiStateHost(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Nada por aqui ainda.",
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is UiState.Content -> content(state.data)
        UiState.Loading -> CenteredMessage(modifier) {
            MorphingHexagon(Modifier.size(Sizing.loadingIndicator))
            SlowLoadingHint()
        }
        UiState.Empty -> CenteredMessage(modifier) {
            MessageText(emptyMessage)
        }
        is UiState.Error -> CenteredMessage(modifier) {
            MessageText(state.kind.userMessage())
            TextButton(onClick = onRetry) {
                Text("Tentar novamente", color = AppTheme.colors.primary)
            }
        }
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier, body: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize().padding(Spacing.screen), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            body()
        }
    }
}

@Composable
private fun MessageText(text: String) {
    Text(
        text = text,
        color = AppTheme.colors.onSurfaceMuted,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}

private fun ErrorKind.userMessage(): String = when (this) {
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
    ErrorKind.UNKNOWN -> "Algo deu errado."
}

@Composable
private fun SlowLoadingHint() {
    val hint = LocalSlowLoadingHint.current ?: return
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SLOW_LOADING_DELAY_MILLIS)
        visible = true
    }
    if (visible) MessageText(hint)
}
