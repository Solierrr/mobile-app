package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing

internal data class ErrorPresentation(val title: String, val description: String, val canRetry: Boolean = true)

internal fun ErrorKind.presentation(): ErrorPresentation = when (this) {
    ErrorKind.CONNECTIVITY -> ErrorPresentation("Você está sem conexão", "Verifique sua internet e tente novamente. As informações já salvas continuam disponíveis no aplicativo.")
    ErrorKind.TIMEOUT -> ErrorPresentation("A resposta está demorando", "Não conseguimos carregar estas informações a tempo. Tente novamente em instantes.")
    ErrorKind.UNAUTHORIZED -> ErrorPresentation("Precisamos verificar seu acesso", "Sua sessão não pôde ser validada. Volte e verifique sua conta. Se necessário, entre novamente.", false)
    ErrorKind.FORBIDDEN -> ErrorPresentation("Este conteúdo é restrito", "Sua conta não tem permissão para acessar estas informações.", false)
    ErrorKind.NOT_FOUND -> ErrorPresentation("Não encontramos este conteúdo", "Ele pode ter sido removido ou não estar mais disponível para sua conta.", false)
    ErrorKind.CONFLICT -> ErrorPresentation("As informações mudaram", "Atualize os dados antes de tentar salvar novamente.")
    ErrorKind.VALIDATION -> ErrorPresentation("Revise as informações", "Alguns dados precisam ser corrigidos antes de continuar.", false)
    ErrorKind.RATE_LIMITED -> ErrorPresentation("Vamos esperar um instante", "Foram feitas muitas tentativas. Aguarde um pouco antes de tentar novamente.")
    ErrorKind.NOT_SUPPORTED -> ErrorPresentation("Este recurso está em preparação", "Esta função ainda não está integrada ao aplicativo. Você pode continuar usando os recursos já disponíveis.", false)
    ErrorKind.SERVER, ErrorKind.SERIALIZATION, ErrorKind.UNKNOWN -> ErrorPresentation("Não foi possível carregar", "Tivemos um problema ao buscar estas informações. Tente novamente em instantes.")
}

@Composable
fun StateMessage(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    animateBrand: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(Spacing.screen), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 360.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            BrandLoader(Modifier.size(96.dp), rotateShape = animateBrand)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(title, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.onSurface, textAlign = TextAlign.Center)
                Text(description, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.onSurfaceMuted, textAlign = TextAlign.Center)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (actionLabel != null && onAction != null) PrimaryButton(actionLabel, onAction)
                if (onBack != null) PrimaryButton("Voltar", onBack, secondary = onAction != null)
            }
        }
    }
}
