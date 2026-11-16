package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Motion
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing

@Composable
fun Skeleton(modifier: Modifier = Modifier) {
    val reduced = Settings.Global.getFloat(LocalContext.current.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    val opacity = if (reduced) 0.08f else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val alpha by transition.animateFloat(0.06f, 0.12f, infiniteRepeatable(tween(Motion.placeholderMillis, easing = LinearEasing), RepeatMode.Reverse), label = "opacity")
        alpha
    }
    Box(modifier.background(AppTheme.colors.onSurface.copy(alpha = opacity), RoundedCornerShape(Radius.medium)).clearAndSetSemantics {})
}

@Composable
fun ContentSkeleton(modifier: Modifier = Modifier, cards: Boolean = false) {
    Column(modifier.fillMaxWidth().padding(Spacing.screen).clearAndSetSemantics { contentDescription = "Carregando conteúdo" }, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Skeleton(Modifier.fillMaxWidth(0.5f).height(Sizing.headerButton))
        repeat(if (cards) 3 else 4) {
            if (cards) Skeleton(Modifier.fillMaxWidth().height(Sizing.projectCardHeight))
            Skeleton(Modifier.fillMaxWidth(0.8f).height(Spacing.lg))
            Skeleton(Modifier.fillMaxWidth(0.55f).height(Spacing.md))
        }
    }
}
