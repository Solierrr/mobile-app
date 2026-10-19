package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
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
) {
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        MorphingHexagon(Modifier.fillMaxSize())
        Image(
            painter = painterResource(R.drawable.ic_hands),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(HANDS_SCALE),
            contentScale = ContentScale.Fit,
        )
    }
}

private const val HANDS_SCALE = 0.52f

/** Tamanho proporcional à largura da tela, para a marca ter a mesma presença em qualquer aparelho. */
@Composable
fun BrandLoader(
    widthFraction: Float,
    modifier: Modifier = Modifier,
    minSize: Int = MIN_SIZE_DP,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val side = maxOf((screenWidth * widthFraction).toInt(), minSize).dp
    BrandLoader(modifier.size(side))
}

const val SPLASH_WIDTH_FRACTION = 0.5f
const val LOADING_WIDTH_FRACTION = 0.34f
private const val MIN_SIZE_DP = 96
