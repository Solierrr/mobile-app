package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R

/**
 * Marca animada: o hexágono orgânico que se transforma (como no web) com as mãos da logo por cima.
 * Serve de splash e de indicador de carregamento; o tamanho vem do [modifier].
 */
@Composable
fun BrandLoader(
    modifier: Modifier = Modifier,
    rotateShape: Boolean = false,
) {
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        MorphingHexagon(Modifier.fillMaxSize(), rotateShape = rotateShape)
        Image(
            painter = painterResource(R.drawable.ic_hands),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(HANDS_SCALE),
            contentScale = ContentScale.Fit,
        )
    }
}

private const val HANDS_SCALE = 0.52f

/** Tamanho da marca na splash. O vetor `splash_brand` da splash nativa usa a mesma medida, para não haver salto na troca. */
val SPLASH_BRAND_SIZE = 180.dp
