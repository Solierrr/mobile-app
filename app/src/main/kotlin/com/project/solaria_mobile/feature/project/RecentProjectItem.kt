package com.project.solaria_mobile.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.project.domain.ProjectSummary

@Composable
fun RecentProjectItem(project: ProjectSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), color = AppTheme.colors.inputFill) {
        Row(Modifier.padding(vertical = Spacing.md, horizontal = Spacing.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ProjectCardImage(project.id, Modifier.width(64.dp).height(72.dp).clip(RoundedCornerShape(Radius.medium)), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(project.displayName, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Column {
                    if (project.companyName.isNotBlank()) Text(project.companyName, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (project.place.isNotBlank()) Text(project.place, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(painterResource(AppIcons.chevron_right), null, Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurfaceMuted)
        }
    }
}

internal val ProjectSummary.displayName: String
    get() = if (companyName.isBlank()) name else name.substringBefore(" - $companyName").trim().ifBlank { name }
