package com.project.solaria_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.theme.SolariamobileTheme
import com.project.solaria_mobile.presentation.navigation.AppNavHost

/**
 * Composition root. Equivalent to `index.tsx`/`main.ts` mounting `<App />`
 * inside a theme/router provider tree in a React/Vue SPA: it sets up the
 * theme and hands off all further UI decisions to `AppNavHost`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SolariamobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
