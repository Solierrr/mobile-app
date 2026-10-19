package com.project.solaria_mobile.core.designsystem.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_EDGE = 1600

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
    val image by produceState<ImageBitmap?>(null, key) {
        value = load(key)?.let { bytes -> withContext(Dispatchers.Default) { decodeSampled(bytes) } }
    }
    val bitmap = image
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = description, modifier = modifier, contentScale = contentScale)
    } else {
        val background = if (placeholder) Modifier.background(AppTheme.colors.onSurfaceMuted.copy(alpha = 0.12f)) else Modifier
        Box(modifier = modifier.then(background)) { fallback() }
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
