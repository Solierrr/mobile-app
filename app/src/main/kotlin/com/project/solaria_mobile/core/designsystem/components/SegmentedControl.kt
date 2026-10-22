package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing

@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.background(AppTheme.colors.inputFill, RoundedCornerShape(Radius.medium)).padding(Spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        options.forEachIndexed { index, label ->
            Surface(onClick = { onSelect(index) }, shape = RoundedCornerShape(Radius.medium),
                color = if (index == selectedIndex) AppTheme.colors.surface else AppTheme.colors.inputFill,
                modifier = Modifier.weight(1f).semantics { role = Role.Tab; selected = index == selectedIndex }) {
                Box(Modifier.defaultMinSize(minHeight = Sizing.touchTarget).padding(horizontal = Spacing.xs, vertical = Spacing.xs), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurface,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
