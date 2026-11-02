package com.project.solaria_mobile.feature.communication

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.project.solaria_mobile.core.designsystem.theme.Brand
import com.project.solaria_mobile.feature.communication.domain.AvatarTone

/** Fundo pastel de cada tom de avatar; a silhueta de usuário usa a versão saturada do mesmo tom ([toContentColor]). */
@Composable
fun AvatarTone.toColor(): Color = when (this) {
    AvatarTone.ORANGE -> Brand.tone100
    AvatarTone.BLUE -> Color(0xFFDCEBFA)
    AvatarTone.GREEN -> Color(0xFFE4F1D6)
    AvatarTone.PINK -> Color(0xFFF8E1EB)
}

@Composable
fun AvatarTone.toContentColor(): Color = when (this) {
    AvatarTone.ORANGE -> Brand.base
    AvatarTone.BLUE -> Color(0xFF378ADD)
    AvatarTone.GREEN -> Color(0xFF639922)
    AvatarTone.PINK -> Color(0xFFD4537E)
}
