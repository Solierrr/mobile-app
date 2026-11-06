package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing

/** Opção que se liga e desliga, em formato de pílula; várias podem ficar ligadas ao mesmo tempo. */
@Composable
fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected; role = Role.Checkbox },
        enabled = enabled,
        shape = RoundedCornerShape(Radius.pill),
        color = if (selected) AppTheme.colors.action else AppTheme.colors.inputFill,
    ) {
        Text(
            label,
            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            color = if (selected) AppTheme.colors.onAccent else AppTheme.colors.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
