package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing

@Composable
fun ScreenTitle(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes trailingIcon: Int? = null,
    trailingLabel: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        RoundIconButton(
            icon = LucideR.drawable.lucide_ic_chevron_left,
            contentDescription = "Voltar",
            onClick = onBack,
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = AppTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null && onTrailingClick != null) {
            RoundIconButton(
                icon = trailingIcon,
                contentDescription = trailingLabel ?: title,
                onClick = onTrailingClick,
            )
        }
    }
}
