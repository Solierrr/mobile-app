package com.project.solaria_mobile.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.solaria_mobile.core.designsystem.components.BrandLoader
import com.project.solaria_mobile.core.designsystem.components.SPLASH_BRAND_SIZE
import com.project.solaria_mobile.core.designsystem.theme.AppTheme

/** Ocupa a janela inteira, atrás das barras do sistema, para a marca ficar no mesmo ponto da splash nativa. */
@Composable
fun SplashScreen() {
    val view = LocalView.current
    var origin by remember {
        val bars = ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.systemBars())
        mutableStateOf(IntOffset(bars?.left ?: 0, bars?.top ?: 0))
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow().round() }) {
        Box(
            Modifier
                .layout { measurable, _ ->
                    val window = view.rootView
                    val placeable = measurable.measure(Constraints.fixed(window.width, window.height))
                    layout(0, 0) { placeable.place(-origin.x, -origin.y) }
                }
                .background(AppTheme.colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            BrandLoader(Modifier.size(SPLASH_BRAND_SIZE))
        }
    }
}
