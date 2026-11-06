package com.project.solaria_mobile.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

private val LocalContainerFill = staticCompositionLocalOf { Color.Unspecified }

/** Fundo branco das páginas principais. */
@Composable
fun Modifier.appBackground(): Modifier = background(AppTheme.colors.surface)

/** Sobre o fundo branco da página, blocos e controles usam o cinza dos campos, como nas sheets. */
@Composable
fun ProvidePageContainers(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContainerFill provides AppTheme.colors.inputFill, content = content)
}

/** Superfícies brancas, como sheets, voltam a usar o cinza dos campos nos blocos internos. */
@Composable
fun ProvideSurfaceContainers(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContainerFill provides AppTheme.colors.inputFill, content = content)
}

@Composable
fun containerFill(): Color = LocalContainerFill.current.takeOrElse { AppTheme.colors.inputFill }
