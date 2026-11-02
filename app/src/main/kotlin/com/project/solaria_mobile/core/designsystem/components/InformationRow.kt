package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing

@Composable
fun InformationRow(title: String, @DrawableRes icon: Int, subtitle: String? = null, trailing: String? = null) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.medium), color = AppTheme.colors.inputFill) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(painterResource(icon), null, Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurface)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted) }
            }
            trailing?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted) }
        }
    }
}
