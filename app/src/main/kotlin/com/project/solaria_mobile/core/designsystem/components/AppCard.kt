package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.containerFill

/**
 * Cartão padrão: superfície clara com cantos arredondados, clicável quando recebe [onClick]. O conteúdo ganha
 * [contentPadding]; passe `PaddingValues(0.dp)` para um cartão com imagem até a borda.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    container: Color = containerFill(),
    shape: Shape = RoundedCornerShape(Radius.large),
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
    content: @Composable () -> Unit,
) {
    val inner: @Composable () -> Unit = { Column(Modifier.padding(contentPadding)) { content() } }
    if (onClick == null) Surface(modifier.fillMaxWidth(), shape = shape, color = container, content = inner)
    else Surface(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth(), shape = shape, color = container, content = inner)
}
