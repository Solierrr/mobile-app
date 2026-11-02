package com.project.solaria_mobile.core.designsystem.components

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Motion
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import androidx.compose.ui.unit.dp

private val LocalSkeletonPulse = staticCompositionLocalOf<State<Float>?> { null }

/** Opacidade animada lida só no desenho, para o pulso não recompor as peças. Parada quando as animações do aparelho estão desligadas. */
@Composable
private fun rememberSkeletonPulse(): State<Float> {
    val reduced = Settings.Global.getFloat(LocalContext.current.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    if (reduced) return remember { mutableFloatStateOf(0.08f) }
    val transition = rememberInfiniteTransition(label = "skeleton")
    return transition.animateFloat(0.06f, 0.12f, infiniteRepeatable(tween(Motion.placeholderMillis, easing = LinearEasing), RepeatMode.Reverse), label = "opacity")
}

@Composable
fun Skeleton(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(Radius.medium)) {
    val pulse = LocalSkeletonPulse.current ?: rememberSkeletonPulse()
    val color = AppTheme.colors.onSurface
    Box(
        modifier
            .drawBehind { drawOutline(shape.createOutline(size, layoutDirection, this), color.copy(alpha = pulse.value)) }
            .clearAndSetSemantics {},
    )
}

/** Agrupa as peças de um esqueleto num único pulso e numa única descrição de acessibilidade. */
@Composable
fun SkeletonGroup(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Spacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    val loadingDescription = stringResource(R.string.sk_carregando_conteudo)
    CompositionLocalProvider(LocalSkeletonPulse provides rememberSkeletonPulse()) {
        Column(modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = loadingDescription }, verticalArrangement = verticalArrangement, content = content)
    }
}

@Composable
fun SkeletonText(widthFraction: Float, modifier: Modifier = Modifier, height: Dp = Spacing.md) {
    Skeleton(modifier.fillMaxWidth(widthFraction).height(height), RoundedCornerShape(Radius.small))
}

/** Linha de lista: imagem ou avatar à esquerda e linhas de texto, na geometria dos itens reais. */
@Composable
fun SkeletonListRow(modifier: Modifier = Modifier, leading: Dp = Sizing.avatar, circle: Boolean = true, lines: Int = 2) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Skeleton(Modifier.size(leading), if (circle) CircleShape else RoundedCornerShape(Radius.medium))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SkeletonText(0.6f, height = Spacing.lg)
            repeat(lines - 1) { SkeletonText(if (it == 0) 0.85f else 0.45f) }
        }
    }
}

/** Lista com busca opcional, para telas de lista como Conversas e Conexões. */
@Composable
fun ListSkeleton(modifier: Modifier = Modifier, search: Boolean = false, rows: Int = 6, leading: Dp = Sizing.avatar, circle: Boolean = true) {
    SkeletonGroup(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        if (search) Skeleton(Modifier.fillMaxWidth().height(Sizing.touchTarget), RoundedCornerShape(Radius.pill))
        repeat(rows) { SkeletonListRow(leading = leading, circle = circle) }
    }
}

@Composable
fun ContentSkeleton(modifier: Modifier = Modifier, cards: Boolean = false) {
    SkeletonGroup(modifier.padding(Spacing.screen)) {
        SkeletonText(0.5f, height = Sizing.headerButton)
        repeat(if (cards) 3 else 4) {
            if (cards) Skeleton(Modifier.fillMaxWidth().height(Sizing.projectCardHeight))
            SkeletonText(0.8f, height = Spacing.lg)
            SkeletonText(0.55f)
        }
    }
}

/** Página com foto no topo e painel sobreposto com avatar, nome e ações, como Perfil. */
@Composable
fun MediaPageSkeleton(mediaHeight: Dp, modifier: Modifier = Modifier) {
    SkeletonGroup(modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Skeleton(Modifier.fillMaxWidth().height(mediaHeight), RoundedCornerShape(0.dp))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Skeleton(Modifier.size(Sizing.avatarLarge), CircleShape)
            SkeletonText(0.6f, height = Spacing.xl)
            SkeletonText(0.4f)
            repeat(3) { Skeleton(Modifier.fillMaxWidth().height(Sizing.actionHeight)) }
        }
    }
}

/** Grupos de rótulo e valor com título discreto, como a tela de diagnóstico. */
@Composable
fun FieldListSkeleton(groups: List<Int>, modifier: Modifier = Modifier) {
    SkeletonGroup(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        groups.forEach { fields ->
            SkeletonText(0.3f, height = Spacing.lg)
            repeat(fields) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    SkeletonText(0.25f)
                    SkeletonText(0.7f)
                }
            }
        }
    }
}

/** Título com texto de apoio, para detalhes curtos como o de um chamado. */
@Composable
fun TitleBlockSkeleton(modifier: Modifier = Modifier, lines: Int = 2) {
    SkeletonGroup(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SkeletonText(0.7f, height = Spacing.xl)
        repeat(lines) { SkeletonText(if (it == lines - 1) 0.55f else 0.9f) }
    }
}

/** Área do mapa com o painel inferior recolhido, na mesma posição do resumo dos projetos. */
@Composable
fun MapSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Skeleton(Modifier.fillMaxWidth().weight(1f), RoundedCornerShape(Radius.large))
        SkeletonText(0.4f, height = Spacing.lg)
        repeat(2) { SkeletonListRow(leading = Sizing.avatar, circle = false) }
    }
}

/** Contagem de imagens e fotos na altura usada pela galeria do projeto. */
@Composable
fun GallerySkeleton(modifier: Modifier = Modifier, photoHeight: Dp = Sizing.coverHeight) {
    SkeletonGroup(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SkeletonText(0.2f)
        repeat(2) { Skeleton(Modifier.fillMaxWidth().height(photoHeight), RoundedCornerShape(Radius.large)) }
    }
}
