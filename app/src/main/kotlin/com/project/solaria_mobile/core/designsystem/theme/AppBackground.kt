package com.project.solaria_mobile.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

private val PageTop = Color(0xFFFBFBFB)
private val PageMiddle = Color(0xFFF5F5F6)
private val PageBottom = Color(0xFFECECEE)

private val LocalContainerFill = staticCompositionLocalOf { Color.Unspecified }

/** Degradê neutro e estático das páginas: mais claro no topo, onde ficam cabeçalho e ações. */
fun Modifier.appBackground(): Modifier = drawWithCache {
    val brush = Brush.verticalGradient(0f to PageTop, 0.45f to PageMiddle, 1f to PageBottom)
    onDrawBehind { drawRect(brush) }
}

/** Blocos desenhados direto sobre o fundo da página ficam brancos para não se perderem no degradê. */
@Composable
fun ProvidePageContainers(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContainerFill provides AppTheme.colors.surface, content = content)
}

/** Superfícies brancas, como sheets, voltam a usar o cinza dos campos nos blocos internos. */
@Composable
fun ProvideSurfaceContainers(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContainerFill provides AppTheme.colors.inputFill, content = content)
}

@Composable
fun containerFill(): Color = LocalContainerFill.current.takeOrElse { AppTheme.colors.inputFill }
