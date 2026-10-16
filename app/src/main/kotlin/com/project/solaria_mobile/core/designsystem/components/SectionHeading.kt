package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.theme.SolariaGreen
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted

@Composable
fun SectionHeading(
    text: String,
    modifier: Modifier = Modifier,
    faded: Boolean = false,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            color = if (faded) SolariaInkMuted.copy(alpha = 0.62f) else SolariaInkMuted,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = SolariaGreen,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}
