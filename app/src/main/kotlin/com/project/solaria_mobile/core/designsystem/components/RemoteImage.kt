package com.project.solaria_mobile.core.designsystem.components

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

private const val MAX_EDGE = 1600

private sealed interface ImageState {
    data object Loading : ImageState
    data object Missing : ImageState
    data object Failed : ImageState
    data class Ready(val bitmap: ImageBitmap) : ImageState
}

@Composable
fun RemoteImage(
    key: String,
    load: suspend (String) -> ByteArray?,
    description: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Boolean = true,
    fallback: @Composable () -> Unit = {},
) {
    var attempt by remember(key) { mutableIntStateOf(0) }
    val image by produceState<ImageState>(ImageState.Loading, key, attempt) {
        value = ImageState.Loading
        value = try {
            val bytes = load(key)
            if (bytes == null) ImageState.Missing else {
                withContext(Dispatchers.Default) { decodeSampled(bytes) }?.let(ImageState::Ready) ?: ImageState.Failed
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ImageState.Failed
        }
    }
    val loaded = image as? ImageState.Ready
    if (loaded != null) {
        Image(bitmap = loaded.bitmap, contentDescription = description, modifier = modifier, contentScale = contentScale)
    } else {
        val background = if (placeholder) Modifier.background(AppTheme.colors.onSurfaceMuted.copy(alpha = 0.12f)) else Modifier
        Box(modifier = modifier.then(background), contentAlignment = Alignment.Center) {
            fallback()
            if (placeholder) when (image) {
                ImageState.Loading -> Skeleton(Modifier.fillMaxSize())
                ImageState.Missing -> Icon(painterResource(AppIcons.images), stringResource(R.string.pd_sem_imagem_cadastrada), Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurfaceMuted)
                ImageState.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    RoundIconButton(AppIcons.refresh, stringResource(R.string.ri_tentar_carregar_imagem_novamente), { attempt++ })
                    Text(stringResource(R.string.ps_imagem_indisponivel), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                is ImageState.Ready -> Unit
            }
        }
    }
}

internal fun decodeSampled(bytes: ByteArray): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (bounds.outWidth / sample > MAX_EDGE || bounds.outHeight / sample > MAX_EDGE) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
}
