package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing

/**
 * Linha de lista padrão: [leading] (avatar, imagem), título, até duas linhas de apoio e [trailing] (horário, seta).
 * Sem fundo por padrão; passe [container] para destacá-la sobre a página.
 */
@Composable
fun ListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    container: Color = Color.Transparent,
    shape: Shape = RoundedCornerShape(Radius.medium),
    // O recuo lateral deixa o destaque ao tocar passar da foto e do horário, em vez de colar neles.
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.sm),
    titleMaxLines: Int = 1,
    subtitleMaxLines: Int = 1,
    enabled: Boolean = true,
) {
    Surface(modifier = modifier.fillMaxWidth(), onClick = onClick, enabled = enabled, shape = shape, color = container) {
        Row(
            Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall, maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) Text(subtitle, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium, maxLines = subtitleMaxLines, overflow = TextOverflow.Ellipsis)
                if (!supporting.isNullOrBlank()) Text(supporting, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            trailing?.invoke()
        }
    }
}
