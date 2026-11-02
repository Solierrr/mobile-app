package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Sizing

/** Tom do próprio fundo, um pouco mais escuro (ou mais claro, em fundo escuro), para o ícone aparecer sem destoar. */
private fun Color.shifted(): Color = if (luminance() > 0.5f) lerp(this, Color.Black, 0.3f) else lerp(this, Color.White, 0.3f)

/** Ícone de imagem que indica que não há imagem, na cor do [background] sobre o qual ele aparece. */
@Composable
internal fun MissingImageIcon(background: Color, size: Dp = Sizing.icon * 1.5f) {
    Icon(painterResource(AppIcons.images), stringResource(R.string.pd_sem_imagem_cadastrada), Modifier.size(size), tint = background.shifted())
}

/** Lugar de uma imagem que não existe (capa, banner): o fundo de sempre com o ícone de imagem em tom escurecido. */
@Composable
fun MissingImage(modifier: Modifier = Modifier, background: Color = AppTheme.colors.mapPlaceholder) {
    Box(modifier.background(background), contentAlignment = Alignment.Center) { MissingImageIcon(background) }
}
