package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.theme.Motion

/**
 * Anima a entrada de algo que passa a existir depois da tela pronta (campo condicional, aviso de erro, formulário que se abre).
 * Usa-se dentro do `if` que decide mostrar; a saída continua imediata para o resto da tela não esperar o fim da animação.
 */
@Composable
fun Appear(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(Motion.appearMillis)) + expandVertically(tween(Motion.appearMillis)),
    ) { content() }
}
