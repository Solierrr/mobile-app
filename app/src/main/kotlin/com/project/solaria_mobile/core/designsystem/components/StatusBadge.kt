package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing

@Composable
fun StatusBadge(text: String, modifier: Modifier = Modifier, tint: Color = AppTheme.colors.onSurfaceMuted) {
    if (text.isBlank()) return
    var wrapped by remember(text) { mutableStateOf(false) }
    Surface(modifier, color = tint.copy(alpha = 0.08f), shape = RoundedCornerShape(if (wrapped) Radius.medium else Radius.pill)) {
        Text(localizedStatus(text), Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            color = tint, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
            onTextLayout = { wrapped = it.lineCount > 1 })
    }
}
