package com.project.solaria_mobile

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import com.project.solaria_mobile.core.offline.OfflineStatus
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.project.solaria_mobile.BuildConfig
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.components.LocalSlowLoadingHint
import com.project.solaria_mobile.feature.auth.AuthViewModel
import com.project.solaria_mobile.presentation.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import com.project.solaria_mobile.feature.communication.data.MessageOutbox
import javax.inject.Inject

/**
 * Composition root. Equivalent to `index.tsx`/`main.ts` mounting `<App />`
 * inside a theme/router provider tree in a React/Vue SPA: it sets up the
 * theme and hands off all further UI decisions to `AppNavHost`.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    @Inject lateinit var messageOutbox: MessageOutbox
    @Inject lateinit var pushRegistration: com.project.solaria_mobile.feature.communication.data.PushRegistration
    private val openInbox = androidx.compose.runtime.mutableStateOf(false)
    private val notificationId = androidx.compose.runtime.mutableStateOf<String?>(null)
    private var splashSettled = false

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.project.solaria_mobile.core.locale.AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashStart = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            !splashSettled && SystemClock.uptimeMillis() - splashStart < SPLASH_MAX_MILLIS
        }
        super.onCreate(savedInstanceState)
        openInbox.value = savedInstanceState?.getBoolean("openInbox") == true
        notificationId.value = savedInstanceState?.getString("notificationId")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        window.isNavigationBarContrastEnforced = false
        handleLoginCallback(intent)
        handleNotification(intent)
        messageOutbox.start()
        pushRegistration.start()
        setContent {
            CompositionLocalProvider(LocalSlowLoadingHint provides slowLoadingHint()) {
                AppTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                    ) { innerPadding ->
                        OfflineStatus(Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
                            AppNavHost(authViewModel = authViewModel, modifier = Modifier.weight(1f), onSplashSettled = { splashSettled = true }, openInbox = openInbox.value, onInboxOpened = { openInbox.value = false }, notificationId = notificationId.value, onNotificationHandled = { notificationId.value = null })
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val SPLASH_MAX_MILLIS = 3000L
    }

    private fun slowLoadingHint(): String? =
        if (BuildConfig.ENVIRONMENT == "qa") "Acordando o servidor de QA, isso pode levar até 1 minuto." else null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLoginCallback(intent)
        handleNotification(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("openInbox", openInbox.value)
        outState.putString("notificationId", notificationId.value)
        super.onSaveInstanceState(outState)
    }

    private fun handleNotification(intent: Intent?) {
        val key = com.project.solaria_mobile.feature.communication.data.SolariaMessagingService.OPEN_INBOX
        if (intent?.getBooleanExtra(key, false) == true) {
            openInbox.value = true
            notificationId.value = intent.getStringExtra("notificationId")?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) }
            intent.removeExtra(key)
            intent.removeExtra("notificationId")
        }
    }

    private fun handleLoginCallback(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.host != "auth" || data.path != "/callback") return
        authViewModel.onLoginCallback(data.getQueryParameter("code"), data.getQueryParameter("state"))
        intent.data = null
    }
}
