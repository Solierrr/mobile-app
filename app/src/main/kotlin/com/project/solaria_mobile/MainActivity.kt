package com.project.solaria_mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.feature.auth.AuthViewModel
import com.project.solaria_mobile.presentation.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Composition root. Equivalent to `index.tsx`/`main.ts` mounting `<App />`
 * inside a theme/router provider tree in a React/Vue SPA: it sets up the
 * theme and hands off all further UI decisions to `AppNavHost`.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleLoginCallback(intent)
        setContent {
            AppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) { innerPadding ->
                    AppNavHost(authViewModel = authViewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLoginCallback(intent)
    }

    private fun handleLoginCallback(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.host != "auth" || data.path != "/callback") return
        authViewModel.onLoginCallback(data.getQueryParameter("code"), data.getQueryParameter("state"))
        intent.data = null
    }
}
