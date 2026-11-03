package com.project.solaria_mobile.feature.communication

import android.media.MediaRecorder
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.communication.data.mediaLimit
import com.project.solaria_mobile.feature.communication.domain.MessageAttachment
import java.io.File
import java.util.UUID

/** A gravação só começa com permissão; parar, sair da tela ou mandar o app ao fundo sempre libera o microfone. */
@Composable
internal fun AudioRecordingSheet(onDismiss: () -> Unit, onAttachment: (MessageAttachment) -> Unit, onInvalid: () -> Unit) {
    val context = LocalContext.current
    val file = remember { File(File(context.filesDir, "chat-media").apply { mkdirs() }, "audio-${UUID.randomUUID()}.m4a") }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var saved by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    fun stop() {
        val active = recorder ?: return
        recorder = null
        val valid = runCatching { active.stop() }.isSuccess
        active.release()
        saved = valid && file.length() in 1..mediaLimit("AUDIO")
        if (!saved) { file.delete(); failed = true }
    }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        @Suppress("DEPRECATION")
        val active = MediaRecorder()
        runCatching {
            active.setAudioSource(MediaRecorder.AudioSource.MIC)
            active.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            active.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            active.setAudioEncodingBitRate(128_000)
            active.setOutputFile(file.absolutePath)
            active.setMaxFileSize(mediaLimit("AUDIO"))
            active.setOnInfoListener { _, what, _ -> if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) stop() }
            active.prepare()
            active.start()
            recorder = active
        }.onFailure { active.release(); failed = true }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) stop() }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            recorder?.let { runCatching { it.stop() }; it.release() }
            recorder = null
            if (!saved) file.delete()
        }
    }
    BottomSheet(onDismiss = { saved = false; onDismiss() }) {
        Column(Modifier.padding(horizontal = Spacing.screen).padding(top = Spacing.sm, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(stringResource(R.string.ch_gravar_audio), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (failed) R.string.ch_arquivo_invalido else if (saved) R.string.ch_audio_pronto else R.string.ch_gravando_audio), color = AppTheme.colors.onSurfaceMuted)
            if (recorder != null) PrimaryButton(stringResource(R.string.ch_parar_gravacao), ::stop)
            if (saved) PrimaryButton(stringResource(R.string.ch_enviar_audio), {
                onAttachment(MessageAttachment.File(file.name, file.length(), "audio/mp4", Uri.fromFile(file).toString()))
                onDismiss()
            })
            if (failed) PrimaryButton(stringResource(R.string.ch_cancelar), { onInvalid(); onDismiss() }, secondary = true)
        }
    }
}
