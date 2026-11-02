package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing

/** Linha com título, descrição opcional e interruptor; a linha inteira alterna e é um único alvo de toque. */
@Composable
fun AppSwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, description: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.medium))
            .background(AppTheme.colors.inputFill)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyLarge)
            if (description != null) Text(description, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = AppTheme.colors.action, checkedThumbColor = AppTheme.colors.onAccent))
    }
}

/** Linha com caixa de seleção e rótulo; a linha inteira alterna. */
@Composable
fun AppCheckboxRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(
        modifier.fillMaxWidth().heightIn(min = Sizing.touchTarget).toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Checkbox(checked, onCheckedChange = null, enabled = enabled, colors = CheckboxDefaults.colors(checkedColor = AppTheme.colors.action))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
    }
}

/** Indicador de carregamento centralizado, para o fim de listas que carregam mais itens. */
@Composable
fun InlineLoader(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = Spacing.md), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(Sizing.icon), color = AppTheme.colors.onSurfaceMuted, strokeWidth = 2.dp)
    }
}
