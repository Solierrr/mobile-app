package com.project.solaria_mobile.feature.auth

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.designsystem.components.StateMessage
import com.project.solaria_mobile.core.designsystem.components.presentation
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

    val error = state.error
    when {
        state.busy || state.session == SessionState.SIGNED_IN -> SplashScreen()
        error != null -> {
            val message = error.presentation()
            StateMessage(
                title = if (error == ErrorKind.FORBIDDEN) "Seu acesso precisa de aprovação" else "Não foi possível entrar",
                description = if (error == ErrorKind.FORBIDDEN) "Este aplicativo é destinado a profissionais aprovados. Verifique a situação do seu cadastro antes de entrar novamente." else message.description,
                animateBrand = true,
                actionLabel = "Voltar ao acesso",
                onAction = viewModel::startLogin,
            )
        }
        else -> StateMessage(
            title = "Entre na sua conta",
            description = "Acesse seus projetos e acompanhe seu trabalho na Solaria.",
            actionLabel = "Entrar",
            onAction = viewModel::startLogin,
        )
    }
}
