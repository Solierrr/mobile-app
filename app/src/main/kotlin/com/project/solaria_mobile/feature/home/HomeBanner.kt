package com.project.solaria_mobile.feature.home

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.RemoteImage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fundos prontos do início. O degradê vai do topo à base e um brilho suave no canto dá volume; [darkContent] diz se o
 * texto por cima deve ser escuro (fundos claros) ou branco (fundos escuros).
 */
enum class BannerPreset(val id: String, @StringRes val label: Int, val top: Color, val bottom: Color, val darkContent: Boolean) {
    SUN("sun", R.string.hm_fundo_sol, Color(0xFFFFC9A3), Color(0xFFFF9B52), true),
    SKY("sky", R.string.hm_fundo_ceu, Color(0xFFD7E8F8), Color(0xFF8FBFE8), true),
    LEAF("leaf", R.string.hm_fundo_folha, Color(0xFFDCEECF), Color(0xFF9CC98A), true),
    SAND("sand", R.string.hm_fundo_areia, Color(0xFFF6EBDA), Color(0xFFE3CFB0), true),
    SUNSET("sunset", R.string.hm_fundo_entardecer, Color(0xFFFF8236), Color(0xFF9A3412), false),
    NIGHT("night", R.string.hm_fundo_noite, Color(0xFF3B4252), Color(0xFF1E232B), false);

    companion object {
        fun fromId(id: String?): BannerPreset = entries.firstOrNull { it.id == id } ?: SUN
    }
}

/** O fundo do início: um dos prontos, ou uma imagem da própria pessoa guardada só neste aparelho. */
sealed interface HomeBanner {
    data class Preset(val preset: BannerPreset) : HomeBanner
    data class Custom(val path: String, val version: Long) : HomeBanner

    companion object {
        val Default: HomeBanner = Preset(BannerPreset.SUN)
    }
}

/** Guarda a escolha no aparelho (preferências e um arquivo); nada disso vai para o servidor. */
@Singleton
class HomeBannerStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) {
    private val file get() = File(context.filesDir, FILE_NAME)

    val banner: Flow<HomeBanner> = dataStore.data.map { prefs ->
        val id = prefs[CHOICE]
        if (id == CUSTOM && file.exists()) HomeBanner.Custom(file.absolutePath, prefs[VERSION] ?: 0L) else HomeBanner.Preset(BannerPreset.fromId(id))
    }

    suspend fun select(preset: BannerPreset) {
        dataStore.edit { it[CHOICE] = preset.id }
    }

    suspend fun setCustom(bytes: ByteArray) {
        withContext(Dispatchers.IO) { file.writeBytes(bytes) }
        dataStore.edit {
            it[CHOICE] = CUSTOM
            it[VERSION] = System.currentTimeMillis()
        }
    }

    private companion object {
        const val FILE_NAME = "home-banner"
        const val CUSTOM = "custom"
        val CHOICE = stringPreferencesKey("home_banner")
        val VERSION = longPreferencesKey("home_banner_version")
    }
}

@HiltViewModel
class HomeBannerViewModel @Inject constructor(private val store: HomeBannerStore) : ViewModel() {
    val banner: StateFlow<HomeBanner> = store.banner.stateIn(viewModelScope, SharingStarted.Eagerly, HomeBanner.Default)

    fun select(preset: BannerPreset) {
        viewModelScope.launch { store.select(preset) }
    }

    fun setCustom(bytes: ByteArray) {
        viewModelScope.launch { store.setCustom(bytes) }
    }
}

/** Cor do texto e dos ícones desenhados sobre o fundo. */
@Composable
fun HomeBanner.contentColor(): Color = when (this) {
    is HomeBanner.Preset -> if (preset.darkContent) AppTheme.colors.onSurface else AppTheme.colors.onAccent
    is HomeBanner.Custom -> AppTheme.colors.onAccent
}

/** Degradê do fundo com um brilho no canto superior direito. */
private fun Modifier.presetBackground(preset: BannerPreset): Modifier = drawBehind {
    drawRect(Brush.verticalGradient(listOf(preset.top, preset.bottom)))
    drawRect(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.32f), Color.Transparent),
            center = Offset(size.width * 0.88f, size.height * 0.12f),
            radius = size.width * 0.7f,
        ),
    )
}

@Composable
fun HomeBannerBackground(banner: HomeBanner, modifier: Modifier = Modifier) {
    when (banner) {
        is HomeBanner.Preset -> Box(modifier.presetBackground(banner.preset))
        is HomeBanner.Custom -> Box(modifier.background(AppTheme.colors.inputFill)) {
            RemoteImage(
                // A versão entra na chave para a imagem recarregar quando a pessoa escolhe outra.
                key = "${banner.version}|${banner.path}",
                load = { key -> withContext(Dispatchers.IO) { runCatching { File(key.substringAfter('|')).readBytes() }.getOrNull() } },
                description = null,
                modifier = Modifier.fillMaxSize(),
            )
            // Escurece o topo para o texto branco ler bem sobre qualquer foto.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.42f), Color.Transparent))))
        }
    }
}

/** Escolha do fundo: os prontos, a imagem atual (se for própria) e a galeria. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BannerSheet(current: HomeBanner, onSelect: (BannerPreset) -> Unit, onPickImage: () -> Unit, invalidImage: Boolean, onDismiss: () -> Unit) {
    BottomSheet(onDismiss = onDismiss) {
        val dismiss = LocalSheetDismiss.current
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(R.string.hm_fundo_titulo), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (current is HomeBanner.Custom) Swatch(stringResource(R.string.hm_fundo_sua_imagem), selected = true, onClick = {}) {
                    HomeBannerBackground(current, Modifier.fillMaxSize())
                }
                BannerPreset.entries.forEach { preset ->
                    Swatch(stringResource(preset.label), selected = (current as? HomeBanner.Preset)?.preset == preset, onClick = { onSelect(preset); dismiss() }) {
                        Box(Modifier.fillMaxSize().presetBackground(preset))
                    }
                }
            }
            ActionRow(stringResource(R.string.hm_fundo_galeria), AppIcons.images, { dismiss(); onPickImage() }, compact = true)
            if (invalidImage) Text(stringResource(R.string.hm_fundo_invalido), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Swatch(label: String, selected: Boolean, onClick: () -> Unit, preview: @Composable () -> Unit) {
    Column(
        Modifier.width(SWATCH_WIDTH).clip(RoundedCornerShape(Radius.medium)).clickable(onClick = onClick).semantics { this.selected = selected; role = Role.RadioButton },
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Box(
            Modifier.fillMaxWidth().height(SWATCH_HEIGHT).clip(RoundedCornerShape(Radius.medium))
                .border(if (selected) BorderStroke(2.dp, AppTheme.colors.action) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(Radius.medium)),
        ) { preview() }
        Text(label, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
    }
}

private val SWATCH_WIDTH = 96.dp
private val SWATCH_HEIGHT = 64.dp
