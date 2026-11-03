package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.core.designsystem.components.SkeletonGroup
import com.project.solaria_mobile.core.designsystem.components.Skeleton
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.feature.discovery.domain.GeoPoint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.MapPoint
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ProjectMap
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.images.readPickedImage
import com.project.solaria_mobile.core.images.rememberCameraCapture
import com.project.solaria_mobile.feature.communication.data.mediaKind
import com.project.solaria_mobile.feature.communication.data.mediaLimit
import com.project.solaria_mobile.feature.communication.domain.MessageAttachment
import com.project.solaria_mobile.feature.discovery.currentLocation
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Quem abre cada origem de anexo; vem das telas, porque o resultado do seletor precisa sobreviver ao fechamento da folha. */
class ChatAttachmentSources(val gallery: () -> Unit, val camera: () -> Unit, val document: () -> Unit, val audio: () -> Unit, val video: () -> Unit)

/**
 * Seletores de foto (galeria e câmera) e de documento. A foto é conferida e copiada para o cache do app, para não depender
 * da permissão temporária do seletor; o documento guarda a permissão persistente do sistema.
 */
@Composable
fun rememberChatAttachmentSources(onAttachment: (MessageAttachment) -> Unit, onInvalid: () -> Unit): ChatAttachmentSources {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recording by remember { mutableStateOf(false) }
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recording = true else onInvalid()
    }
    var videoUri by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val video = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { saved ->
        val uri = videoUri?.let(Uri::parse)
        videoUri = null
        if (saved && uri != null) scope.launch {
            val attachment = withContext(Dispatchers.IO) { readDocument(context, uri) }
            if (attachment == null) { runCatching { context.contentResolver.delete(uri, null, null) }; onInvalid() } else onAttachment(attachment)
        } else if (uri != null) runCatching { context.contentResolver.delete(uri, null, null) }
    }
    fun photo(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            val image = withContext(Dispatchers.IO) { copyPhoto(context, uri) }
            if (image == null) onInvalid() else onAttachment(image)
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), ::photo)
    val camera = rememberCameraCapture(::photo)
    val document = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val file = withContext(Dispatchers.IO) { readDocument(context, uri) }
            if (file == null) onInvalid() else onAttachment(file)
        }
    }
    if (recording) AudioRecordingSheet(onDismiss = { recording = false }, onAttachment = onAttachment, onInvalid = onInvalid)
    return remember(gallery, camera, document, microphone, video) {
        ChatAttachmentSources(
            gallery = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            camera = camera,
            document = { document.launch(arrayOf("*/*")) },
            audio = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) recording = true
                else microphone.launch(Manifest.permission.RECORD_AUDIO)
            },
            video = {
                val directory = File(context.filesDir, "chat-media").apply { mkdirs() }
                val file = File(directory, "video-${UUID.randomUUID()}.mp4")
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                videoUri = uri.toString()
                runCatching { video.launch(uri) }.onFailure { videoUri = null; file.delete(); onInvalid() }
            },
        )
    }
}

private fun copyPhoto(context: Context, uri: Uri): MessageAttachment.Image? {
    val (bytes, type) = readPickedImage(context, uri) ?: return null
    val directory = File(context.cacheDir, "chat-media").apply { mkdirs() }
    val file = File(directory, "${UUID.randomUUID()}.${if (type == "image/png") "png" else "jpg"}")
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    return runCatching {
        file.writeBytes(bytes)
        MessageAttachment.Image(Uri.fromFile(file).toString(), sizeBytes = bytes.size.toLong(), mimeType = type,
            width = bounds.outWidth.takeIf { it > 0 }, height = bounds.outHeight.takeIf { it > 0 })
    }.getOrNull()
}

private fun readDocument(context: Context, uri: Uri): MessageAttachment.File? {
    val resolver = context.contentResolver
    runCatching { resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    var name: String? = null
    var size = 0L
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            name = cursor.getString(0)
            size = if (cursor.isNull(1)) 0L else cursor.getLong(1)
        }
    }
    val mime = resolver.getType(uri).orEmpty()
    // O servidor confere o tamanho exato do arquivo e o teto de cada tipo; sem tamanho conhecido não há como declarar o envio.
    val file = MessageAttachment.File(name ?: uri.lastPathSegment.orEmpty().ifBlank { "arquivo" }, size, mime, uri.toString())
    if (size <= 0 || size > mediaLimit(file.mediaKind())) return null
    return file
}

/** Escolha da origem do anexo; a localização abre a própria folha de confirmação. */
@Composable
fun AttachmentSheet(sources: ChatAttachmentSources, onLocation: () -> Unit, onDismiss: () -> Unit) {
    BottomSheet(onDismiss = onDismiss) {
        val dismiss = LocalSheetDismiss.current
        SheetColumn {
            Text(stringResource(R.string.ch_anexar), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
            ActionRow(stringResource(R.string.ch_foto_galeria), AppIcons.images, { dismiss(); sources.gallery() }, compact = true)
            ActionRow(stringResource(R.string.ch_camera), AppIcons.camera, { dismiss(); sources.camera() }, compact = true)
            ActionRow(stringResource(R.string.ch_gravar_audio), AppIcons.mic, { dismiss(); sources.audio() }, compact = true)
            ActionRow(stringResource(R.string.ch_gravar_video), AppIcons.camera, { dismiss(); sources.video() }, compact = true)
            ActionRow(stringResource(R.string.ch_documento), AppIcons.folder, { dismiss(); sources.document() }, compact = true)
            ActionRow(stringResource(R.string.ch_localizacao), AppIcons.map_pin, { dismiss(); onLocation() }, compact = true)
        }
    }
}

private sealed interface Current {
    data object Searching : Current
    data object NoPermission : Current
    data object Failed : Current
    data class Ready(val latitude: Double, val longitude: Double) : Current
}

private data class Target(val latitude: Double, val longitude: Double, val label: String)

/**
 * Escolha do que enviar: a posição atual (padrão) ou um endereço buscado por rua, CEP ou bairro, com o preview no mapa antes
 * de enviar. A permissão de localização só é necessária para a posição atual; a busca funciona sem ela.
 */
@Composable
fun LocationShareSheet(onDismiss: () -> Unit, onSend: (Double, Double, String) -> Unit, search: LocationSearchViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var current by remember { mutableStateOf<Current>(Current.Searching) }
    var attempt by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<GeoPoint?>(null) }
    val found by search.state.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) attempt++ else current = Current.NoPermission
    }
    LaunchedEffect(attempt) {
        if (!hasLocationPermission(context)) {
            if (attempt == 0) permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            else current = Current.NoPermission
            return@LaunchedEffect
        }
        current = Current.Searching
        val location = currentLocation(context)
        current = if (location == null) Current.Failed else Current.Ready(location.latitude, location.longitude)
    }
    BottomSheet(onDismiss = onDismiss) {
        val dismiss = LocalSheetDismiss.current
        SheetColumn {
            Text(stringResource(R.string.ch_enviar_localizacao), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
            SearchField(stringResource(R.string.ch_buscar_endereco), query, { text -> chosen = null; query = text; search.onQueryChange(text) })
            if (chosen == null && query.trim().length >= MIN_SEARCH_LENGTH) {
                when (val result = found) {
                    LocationSearch.Idle, LocationSearch.Searching -> SearchResultsSkeleton()
                    is LocationSearch.Results -> result.places.forEach { place ->
                        ActionRow(place.formattedAddress, AppIcons.map_pin, { chosen = place; search.clear() }, compact = true)
                    }
                    LocationSearch.NotFound ->
                        Text(stringResource(R.string.ch_nenhum_endereco), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                    LocationSearch.Failed ->
                        Text(stringResource(R.string.ch_busca_indisponivel), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val place = chosen
                val ready = current as? Current.Ready
                val target = when {
                    place != null -> Target(place.latitude, place.longitude, place.formattedAddress)
                    ready != null -> Target(ready.latitude, ready.longitude, "")
                    else -> null
                }
                if (target != null) {
                    Box(Modifier.fillMaxWidth().height(Sizing.coverHeight).clip(RoundedCornerShape(Radius.large)), contentAlignment = Alignment.Center) {
                        if (BuildConfig.MAPS_CONFIGURED) {
                            ProjectMap(
                                points = listOf(MapPoint("location", "", target.latitude, target.longitude)),
                                selectedId = "location", onSelect = {}, modifier = Modifier.fillMaxWidth().height(Sizing.coverHeight), interactive = false,
                            )
                        } else StateNotice(stringResource(R.string.ch_localizacao), "%.5f, %.5f".format(target.latitude, target.longitude), icon = AppIcons.map_pin)
                    }
                    // Só o endereço escolhido aparece sob o mapa; a posição atual não precisa de legenda.
                    if (target.label.isNotBlank()) Text(target.label, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                } else when (current) {
                    Current.NoPermission -> {
                        Text(stringResource(R.string.ch_localizacao_sem_permissao), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                        PrimaryButton(stringResource(R.string.ch_permitir), { permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, secondary = true)
                    }
                    Current.Failed -> {
                        Text(stringResource(R.string.ch_localizacao_indisponivel), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
                        PrimaryButton(stringResource(R.string.ch_tentar_de_novo), { attempt++ }, secondary = true)
                    }
                    else -> SkeletonGroup { Skeleton(Modifier.fillMaxWidth().height(Sizing.coverHeight), RoundedCornerShape(Radius.large)) }
                }
                if (place != null) ActionRow(stringResource(R.string.ch_usar_minha_localizacao), AppIcons.map_pin, { chosen = null; query = ""; search.clear() }, compact = true)
                PrimaryButton(
                    stringResource(if (place == null) R.string.ch_enviar_localizacao_atual else R.string.ch_enviar_localizacao),
                    { if (target != null) { dismiss(); onSend(target.latitude, target.longitude, target.label) } },
                    enabled = target != null,
                )
            }
        }
    }
}

private const val MIN_SEARCH_LENGTH = 3
private const val SEARCH_SKELETON_ROWS = 3

/** Linhas de resultado a caminho, na altura das linhas reais. */
@Composable
private fun SearchResultsSkeleton() {
    SkeletonGroup(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        repeat(SEARCH_SKELETON_ROWS) { Skeleton(Modifier.fillMaxWidth().height(Sizing.actionHeight), RoundedCornerShape(Radius.large)) }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

/** Conteúdo das folhas do chat, com a mesma margem e o mesmo espaçamento das outras folhas do app. */
@Composable
private fun SheetColumn(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) { content() }
}
