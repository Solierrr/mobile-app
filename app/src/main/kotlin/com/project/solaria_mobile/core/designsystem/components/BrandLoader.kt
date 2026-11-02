package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
    millisPerShape: Int = DEFAULT_MORPH_MILLIS,
) {
    BoxWithConstraints(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        val shapeHeight = maxWidth * BRAND_SHAPE_SCALE
        MorphingHexagon(Modifier.fillMaxSize(), rotateShape = rotateShape, millisPerShape = millisPerShape)
        // A sombra das mãos usa o desfoque do sistema, que existe a partir do Android 12; antes disso as mãos ficam sem sombra.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Image(
                painter = painterResource(R.drawable.ic_hands),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(HANDS_SCALE)
                    .offset(y = shapeHeight * HANDS_SHADOW_SHIFT)
                    .blur(shapeHeight * HANDS_SHADOW_BLUR, BlurredEdgeTreatment.Unbounded),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.Black.copy(alpha = HANDS_SHADOW_ALPHA)),
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_hands),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(HANDS_SCALE),
            contentScale = ContentScale.Fit,
        )
    }
}

private const val DEFAULT_MORPH_MILLIS = 1400

/** As mãos ocupam 70% da largura do hexágono no Figma. */
private const val HANDS_SCALE = 0.585f
private const val HANDS_SHADOW_SHIFT = 0.0175f
private const val HANDS_SHADOW_BLUR = 0.0436f
private const val HANDS_SHADOW_ALPHA = 0.1f

/** Tamanho da marca na splash. O vetor `splash_emblem` da splash nativa usa a mesma medida, para não haver salto na troca. */
val SPLASH_BRAND_SIZE = 180.dp
