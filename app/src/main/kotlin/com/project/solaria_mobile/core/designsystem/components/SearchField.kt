package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.theme.SolariaGreen
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaInput
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace

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
                painter = painterResource(LucideR.drawable.lucide_ic_search),
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
