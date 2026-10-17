package com.project.solaria_mobile.feature.auth

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.auth.domain.SessionState
import com.project.solaria_mobile.feature.home.SplashScreen

@Composable
fun SessionSplash(viewModel: AuthViewModel, onSignedIn: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effectFlow.collect { effect ->
            when (effect) {
                is AuthEffect.OpenLogin -> CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(effect.url))
            }
        }
    }
    LaunchedEffect(state.session) {
        if (state.session == SessionState.SIGNED_IN) onSignedIn()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SplashScreen()
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            val error = state.error
            when {
                state.busy -> CircularProgressIndicator(color = AppTheme.colors.secondary)
                error != null -> {
                    Text(
                        text = error.loginMessage(),
                        color = AppTheme.colors.onSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = viewModel::startLogin) { Text("Entrar novamente", color = AppTheme.colors.primary) }
                }
                state.session == SessionState.SIGNED_OUT -> TextButton(onClick = viewModel::startLogin) {
                    Text("Entrar", color = AppTheme.colors.primary)
                }
            }
        }
    }
}

private fun ErrorKind.loginMessage(): String = when (this) {
    ErrorKind.CONNECTIVITY -> "Sem conexão. Verifique sua internet."
    ErrorKind.TIMEOUT -> "O servidor demorou para responder."
    ErrorKind.UNAUTHORIZED -> "Não foi possível concluir o login."
    ErrorKind.FORBIDDEN -> "Este app é exclusivo de profissionais aprovados."
    else -> "Não foi possível iniciar o login."
}
