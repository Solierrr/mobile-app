package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.SolariaBlue
import com.project.solaria_mobile.core.designsystem.theme.SolariaGreen
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaInput
import com.project.solaria_mobile.core.designsystem.theme.SolariaOrange
import com.project.solaria_mobile.core.designsystem.theme.SolariaPink
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

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
        horizontalArrangement = Arrangement.spacedBy(SolariaSpace.md),
    ) {
        RoundIconButton(
            icon = com.project.solaria_mobile.R.drawable.lucide_ic_chevron_left,
            contentDescription = "Voltar",
            onClick = onBack,
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = SolariaInk,
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

@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = SolariaSpace.headerButton,
    tint: Color = SolariaInk,
    background: Color = SolariaWhite,
) {
    Surface(
        modifier = modifier.size(size),
        onClick = onClick,
        shape = CircleShape,
        color = background,
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(SolariaSpace.icon),
            )
        }
    }
}

@Composable
fun ActionRow(
    title: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    trailing: String? = null,
    subtitle: String? = null,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(SolariaRadius.pill),
        color = SolariaInput,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = if (compact) 42.dp else SolariaSpace.actionHeight)
                .padding(horizontal = if (compact) SolariaSpace.md else 22.dp, vertical = if (compact) 8.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = SolariaInk,
                modifier = Modifier.size(if (compact) SolariaSpace.iconSmall else SolariaSpace.icon),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    color = SolariaInk,
                    style = if (compact) androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    else androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = SolariaInkMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            when {
                badge != null -> CountBadge(value = badge)
                trailing != null -> Text(
                    text = trailing,
                    color = SolariaInkMuted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun CountBadge(value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(SolariaGreen.copy(alpha = 0.15f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value,
            color = SolariaGreen,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        )
    }
}

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

@Composable
fun SolariaAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = SolariaSpace.avatar,
    color: Color = SolariaOrange,
    online: Boolean = false,
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                color = SolariaWhite,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
        }
        if (online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.3f)
                    .clip(CircleShape)
                    .background(SolariaWhite)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(SolariaGreen),
            )
        }
    }
}

@Composable
fun SearchField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes trailingIcon: Int? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SolariaRadius.pill),
        color = SolariaInput,
    ) {
        Row(
            modifier = Modifier.height(44.dp).padding(horizontal = SolariaSpace.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
        ) {
            Icon(
                painter = painterResource(com.project.solaria_mobile.R.drawable.lucide_ic_search),
                contentDescription = null,
                tint = SolariaInkMuted,
                modifier = Modifier.size(SolariaSpace.iconSmall),
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = SolariaInk),
                cursorBrush = SolidColor(SolariaGreen),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = SolariaInkMuted,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (trailingIcon != null && onTrailingClick != null) {
                IconButton(onClick = onTrailingClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(trailingIcon),
                        contentDescription = "Filtrar",
                        tint = SolariaInkMuted,
                        modifier = Modifier.size(SolariaSpace.iconSmall),
                    )
                }
            }
        }
    }
}

@Composable
fun ListItemDivider(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF4F4F4)))
}

@Composable
fun AvatarPalette(index: Int): Color = when (index % 4) {
    0 -> SolariaOrange
    1 -> SolariaBlue
    2 -> SolariaGreen
    else -> SolariaPink
}

@Composable
fun RowSpacer(width: Dp = SolariaSpace.md) {
    Spacer(modifier = Modifier.width(width))
}

@Composable
fun MapBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color(0xFFE7E7E7))) {
        val w = size.width
        val h = size.height
        val roads = listOf(
            Path().apply { moveTo(-w * 0.1f, h * 0.22f); cubicTo(w * 0.2f, h * 0.35f, w * 0.28f, h * 0.08f, w * 0.54f, h * 0.25f); cubicTo(w * 0.77f, h * 0.39f, w * 0.82f, h * 0.12f, w * 1.1f, h * 0.26f) },
            Path().apply { moveTo(-w * 0.08f, h * 0.58f); cubicTo(w * 0.22f, h * 0.45f, w * 0.32f, h * 0.72f, w * 0.62f, h * 0.55f); cubicTo(w * 0.8f, h * 0.45f, w * 0.84f, h * 0.65f, w * 1.08f, h * 0.51f) },
            Path().apply { moveTo(w * 0.12f, -h * 0.1f); cubicTo(w * 0.32f, h * 0.2f, w * 0.06f, h * 0.52f, w * 0.32f, h * 0.75f); cubicTo(w * 0.47f, h * 0.9f, w * 0.35f, h * 1.05f, w * 0.57f, h * 1.1f) },
            Path().apply { moveTo(w * 0.78f, -h * 0.1f); cubicTo(w * 0.62f, h * 0.18f, w * 0.91f, h * 0.33f, w * 0.7f, h * 0.62f); cubicTo(w * 0.62f, h * 0.78f, w * 0.82f, h * 0.9f, w * 0.71f, h * 1.1f) },
        )
        roads.forEachIndexed { index, path ->
            drawPath(path, color = Color.White, style = Stroke(width = if (index < 2) 5.dp.toPx() else 3.dp.toPx()))
        }
        val mainRoad = Path().apply {
            moveTo(-w * 0.05f, h * 0.82f)
            cubicTo(w * 0.24f, h * 0.63f, w * 0.17f, h * 0.48f, w * 0.44f, h * 0.43f)
            cubicTo(w * 0.73f, h * 0.38f, w * 0.72f, h * 0.21f, w * 1.05f, h * 0.08f)
        }
        drawPath(mainRoad, color = Color(0xFFFFE999), style = Stroke(width = 13.dp.toPx()))
        val water = Path().apply {
            moveTo(-w * 0.05f, h * 0.47f)
            cubicTo(w * 0.2f, h * 0.43f, w * 0.23f, h * 0.27f, w * 0.5f, h * 0.3f)
            cubicTo(w * 0.75f, h * 0.34f, w * 0.73f, h * 0.51f, w * 1.06f, h * 0.42f)
        }
        drawPath(water, color = Color(0xFF9AD4F5), style = Stroke(width = 6.dp.toPx()))
        drawCircle(color = Color(0xFFBDEACB), radius = w * 0.15f, center = androidx.compose.ui.geometry.Offset(w * 0.92f, h * 0.87f))
    }
}
