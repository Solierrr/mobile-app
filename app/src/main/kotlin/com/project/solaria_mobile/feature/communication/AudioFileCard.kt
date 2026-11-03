package com.project.solaria_mobile.feature.communication

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.communication.domain.MessageAttachment

/** Player local de áudio; libera o decoder ao sair da bolha e pausa quando o app vai ao fundo. */
@Composable
internal fun AudioFileCard(file: MessageAttachment.File) {
    val context = LocalContext.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var player by remember(file.url) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(file.url) { mutableStateOf(false) }
    var preparing by remember(file.url) { mutableStateOf(false) }
    var failed by remember(file.url) { mutableStateOf(false) }
    DisposableEffect(file.url, owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && playing) { player?.pause(); playing = false }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); player?.release(); player = null }
    }
    Column(Modifier.width(240.dp).padding(Spacing.xs)) {
        Text(file.name, color = AppTheme.colors.onSurface)
        if (failed) Text(stringResource(R.string.ch_midia_indisponivel), color = AppTheme.colors.onSurfaceMuted)
        PrimaryButton(stringResource(if (playing) R.string.ch_pausar_audio else R.string.ch_reproduzir_audio), {
            val active = player
            when {
                playing -> { active?.pause(); playing = false }
                active != null -> { active.start(); playing = true }
                else -> {
                    preparing = true
                    failed = false
                    val created = MediaPlayer()
                    player = created
                    created.setOnPreparedListener {
                        preparing = false
                        if (owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) { it.start(); playing = true }
                    }
                    created.setOnCompletionListener { playing = false }
                    created.setOnErrorListener { broken, _, _ ->
                        broken.release(); player = null; preparing = false; playing = false; failed = true; true
                    }
                    runCatching {
                        created.setDataSource(context, Uri.parse(file.url))
                        created.prepareAsync()
                    }.onFailure { created.release(); player = null; preparing = false; failed = true }
                }
            }
        }, enabled = !preparing, compact = true, secondary = true)
    }
}
