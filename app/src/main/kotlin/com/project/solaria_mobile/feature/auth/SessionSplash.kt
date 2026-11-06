package com.project.solaria_mobile.feature.auth

import kotlinx.coroutines.delay
import com.project.solaria_mobile.feature.home.SplashTiming
import com.project.solaria_mobile.core.designsystem.components.animationsEnabled
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.R
import com.project.solaria_mobile.BuildConfig
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
    val animated = animationsEnabled()
    var settled by remember { mutableStateOf(SplashTiming.played) }
    LaunchedEffect(Unit) {
        if (settled) return@LaunchedEffect
        delay((if (animated) SplashTiming.INTRO_MILLIS else SplashTiming.REDUCED_MILLIS).toLong())
        SplashTiming.played = true
        settled = true
    }
    LaunchedEffect(state.session, settled) {
        if (state.session == SessionState.SIGNED_IN && settled) onSignedIn()
    }

    val error = state.error
    when {
        !settled || state.busy || state.session == SessionState.SIGNED_IN -> SplashScreen()
        error != null -> {
            val message = error.presentation()
            val qa = BuildConfig.ENVIRONMENT == "qa"
            StateMessage(
                title = stringResource(if (error == ErrorKind.FORBIDDEN) R.string.login_forbidden_title else R.string.login_error_title),
                description = when {
                    error == ErrorKind.FORBIDDEN -> stringResource(R.string.login_forbidden_description)
                    // No QA o login real usa a credencial do aparelho; sem ela (ou recusada), a causa mais provável é essa.
                    qa && error == ErrorKind.UNAUTHORIZED -> stringResource(R.string.login_qa_credential)
                    else -> message.description
                },
                animateBrand = true,
                actionLabel = stringResource(R.string.login_back_to_access),
                onAction = viewModel::startLogin,
                secondaryActionLabel = if (qa) stringResource(R.string.login_use_mock) else null,
                onSecondaryAction = if (qa) viewModel::startLoginInMockMode else null,
            )
        }
        else -> StateMessage(
            title = stringResource(R.string.login_title),
            description = stringResource(R.string.login_description) +
                if (BuildConfig.ENVIRONMENT == "qa") stringResource(R.string.login_description_qa) else "",
            actionLabel = stringResource(R.string.login_action),
            onAction = viewModel::startLogin,
            secondaryActionLabel = if (BuildConfig.ENVIRONMENT == "qa") stringResource(R.string.login_use_mock) else null,
            onSecondaryAction = if (BuildConfig.ENVIRONMENT == "qa") viewModel::startLoginInMockMode else null,
        )
    }
}
