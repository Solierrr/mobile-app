package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing

@Composable
fun ActionTile(title: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(Radius.medium), color = AppTheme.colors.inputFill) {
        Column(Modifier.fillMaxWidth().defaultMinSize(minHeight = 96.dp).padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(painterResource(icon), null, Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurface)
            Text(title, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.onSurface)
        }
    }
}
