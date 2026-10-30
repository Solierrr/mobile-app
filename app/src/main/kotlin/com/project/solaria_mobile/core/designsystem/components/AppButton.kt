package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.AppColors
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing

enum class ButtonVariant { Primary, Secondary, Text, Destructive }

enum class ButtonSize { Large, Medium }

data class ButtonPalette(val container: Color, val content: Color, val border: Color?)

fun buttonPalette(variant: ButtonVariant, colors: AppColors): ButtonPalette = when (variant) {
    ButtonVariant.Primary -> ButtonPalette(colors.action, colors.onAccent, null)
    ButtonVariant.Secondary -> ButtonPalette(colors.surface, colors.onSurface, colors.outline)
    ButtonVariant.Text -> ButtonPalette(Color.Transparent, colors.action, null)
    ButtonVariant.Destructive -> ButtonPalette(colors.danger, colors.onAccent, null)
}

fun buttonInteractive(enabled: Boolean, loading: Boolean): Boolean = enabled && !loading

fun buttonMinHeight(size: ButtonSize) = if (size == ButtonSize.Large) Sizing.actionHeight else Sizing.touchTarget

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    fillWidth: Boolean = size == ButtonSize.Large,
    @DrawableRes leadingIcon: Int? = null,
) {
    val palette = buttonPalette(variant, AppTheme.colors)
    val interactive = buttonInteractive(enabled, loading)
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = interactive,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = buttonMinHeight(size))
            .semantics { if (loading) stateDescription = "Carregando" },
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = if (size == ButtonSize.Large) Spacing.md else Spacing.sm),
        shape = RoundedCornerShape(Radius.medium),
        border = palette.border?.let { BorderStroke(Sizing.hairline, it) },
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.container,
            contentColor = palette.content,
            disabledContainerColor = palette.container.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = palette.content.copy(alpha = DISABLED_ALPHA),
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Sizing.iconSmall - 4.dp),
                    strokeWidth = 2.dp,
                    color = palette.content,
                )
                Spacer(Modifier.width(Spacing.xs))
            } else if (leadingIcon != null) {
                Icon(painterResource(leadingIcon), contentDescription = null, modifier = Modifier.size(Sizing.iconSmall - 4.dp))
                Spacer(Modifier.width(Spacing.xs))
            }
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

private const val DISABLED_ALPHA = 0.45f
