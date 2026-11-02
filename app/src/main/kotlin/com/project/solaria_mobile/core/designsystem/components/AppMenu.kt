package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.containerFill

private val MenuMinWidth = 208.dp
private val MenuIconSize = 20.dp
private val MenuGap = Spacing.xs

/** Espaço transparente em volta do cartão, onde a sombra difusa é desenhada. */
private val ShadowRoom = 32.dp
private val ShadowBlur = 28.dp
private val ShadowOffsetY = 8.dp
private const val SHADOW_ALPHA = 0.10f

/**
 * Menu suspenso padrão: um cartão branco de raio grande, sem borda, com uma sombra difusa e baixa que o separa da página (a
 * sombra padrão do Material ficava colada ao cartão e parecia uma borda). O item marcado ganha o fundo cinza dos campos.
 * Use [AppMenuItem] para os itens e [AppMenuDivider] para separar grupos.
 */
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = expanded
    // Mantém o popup até a animação de saída terminar.
    if (!visible.currentState && !visible.targetState) return
    val density = LocalDensity.current
    val position = remember(density) { MenuPosition(density) }
    Popup(popupPositionProvider = position, onDismissRequest = onDismissRequest, properties = PopupProperties(focusable = true)) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(120)) + scaleIn(tween(160), initialScale = 0.92f, transformOrigin = TransformOrigin(0.5f, 0f)),
            exit = fadeOut(tween(90)) + scaleOut(tween(110), targetScale = 0.96f, transformOrigin = TransformOrigin(0.5f, 0f)),
        ) {
            val shape = RoundedCornerShape(Radius.large)
            Column(
                modifier = Modifier.padding(ShadowRoom)
                    .dropShadow(shape, Shadow(radius = ShadowBlur, color = Color.Black, alpha = SHADOW_ALPHA, offset = DpOffset(0.dp, ShadowOffsetY)))
                    .then(modifier)
                    .widthIn(min = MenuMinWidth)
                    .width(IntrinsicSize.Max)
                    .clip(shape)
                    .background(AppTheme.colors.surface)
                    .padding(vertical = Spacing.xs),
                content = content,
            )
        }
    }
}

/**
 * Abaixo do botão que abriu o menu, alinhado à esquerda dele; se não couber à direita, alinha à direita do botão, e se
 * não couber embaixo, abre para cima. Desconta o espaço da sombra para o cartão encostar onde deveria.
 */
private class MenuPosition(private val density: Density) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val room = with(density) { ShadowRoom.roundToPx() }
        val gap = with(density) { MenuGap.roundToPx() }
        val margin = with(density) { Spacing.sm.roundToPx() }
        val card = IntSize(popupContentSize.width - 2 * room, popupContentSize.height - 2 * room)
        var x = anchorBounds.left
        if (x + card.width > windowSize.width - margin) x = anchorBounds.right - card.width
        x = x.coerceIn(margin, (windowSize.width - card.width - margin).coerceAtLeast(margin))
        var y = anchorBounds.bottom + gap
        if (y + card.height > windowSize.height - margin) y = (anchorBounds.top - gap - card.height).coerceAtLeast(margin)
        return IntOffset(x - room, y - room)
    }
}

/**
 * Item do [AppMenu]: ícone opcional à esquerda e texto em peso médio, com destaque arredondado ao tocar. [destructive]
 * pinta o item de vermelho, para ações como excluir.
 */
@Composable
fun AppMenuItem(
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    /** Opção escolhida de uma lista de seleção: fundo cinza e um visto à direita. */
    selected: Boolean = false,
) {
    val color = if (destructive) AppTheme.colors.danger else AppTheme.colors.onSurface
    DropdownMenuItem(
        text = { Text(label, color = color, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = Spacing.xs).clip(RoundedCornerShape(Radius.medium)).background(if (selected) AppTheme.colors.inputFill else Color.Transparent),
        trailingIcon = if (selected) { { Icon(painterResource(com.project.solaria_mobile.core.designsystem.icons.AppIcons.check), null, Modifier.size(MenuIconSize), tint = AppTheme.colors.brandStrong) } } else null,
        leadingIcon = icon?.let { res ->
            { Icon(painterResource(res), contentDescription = null, modifier = Modifier.size(MenuIconSize), tint = if (destructive) color else AppTheme.colors.onSurfaceMuted) }
        },
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
    )
}

/** Linha fina entre grupos de itens do [AppMenu]. */
@Composable
fun AppMenuDivider() {
    HorizontalDivider(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xxs), color = AppTheme.colors.divider)
}
