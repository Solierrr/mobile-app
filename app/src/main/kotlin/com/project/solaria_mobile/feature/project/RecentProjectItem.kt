package com.project.solaria_mobile.feature.project

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.layout.PaddingValues
import com.project.solaria_mobile.core.designsystem.components.ListRow
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
import com.project.solaria_mobile.core.designsystem.theme.containerFill
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.project.domain.ProjectSummary

@Composable
fun RecentProjectItem(project: ProjectSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListRow(
        title = project.displayName,
        subtitle = project.companyName,
        supporting = project.place,
        onClick = onClick,
        modifier = modifier,
        leading = { ProjectCardImage(project.id, Modifier.width(Sizing.thumbnailWidth).height(Sizing.thumbnailHeight).clip(RoundedCornerShape(Radius.medium)), contentScale = ContentScale.Crop) },
        trailing = { Icon(painterResource(AppIcons.chevron_right), null, Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurfaceMuted) },
        container = containerFill(),
        shape = RectangleShape,
        contentPadding = PaddingValues(vertical = Spacing.md, horizontal = Spacing.sm),
        titleMaxLines = 2,
    )
}

internal val ProjectSummary.displayName: String
    get() = if (companyName.isBlank()) name else name.substringBefore(" - $companyName").trim().ifBlank { name }
