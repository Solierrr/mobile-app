@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.project

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.components.AvatarPalette
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.presentation.navigation.AppDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ProjectSheet(val title: String) {
    LOCATION("Informações do local"),
    GALLERY("Imagens do local"),
    AGENDA("Agenda do projeto"),
    CONVERSATIONS("Conversas do projeto"),
    HISTORY("Histórico"),
    TEAM("Envolvidos"),
}

private val sectionModifier = Modifier.fillMaxWidth().height(160.dp)

@Composable
internal fun ProjectSheetContent(
    sheet: ProjectSheet,
    viewModel: ProjectSectionsViewModel,
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    onCopy: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.xxl, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenTitle(sheet.title, onBack)
        when (sheet) {
            ProjectSheet.LOCATION -> LocationSection(viewModel, onNavigate, onCopy)
            ProjectSheet.GALLERY -> GallerySection(viewModel)
            ProjectSheet.AGENDA -> AgendaSection(viewModel)
            ProjectSheet.CONVERSATIONS -> ConversationsSection(onNavigate)
            ProjectSheet.HISTORY -> HistorySection(viewModel)
            ProjectSheet.TEAM -> TeamSection(viewModel, onNavigate)
        }
    }
}

@Composable
private fun LocationSection(viewModel: ProjectSectionsViewModel, onNavigate: (AppDestination) -> Unit, onCopy: (String) -> Unit) {
    val state by viewModel.location.collectAsStateWithLifecycle()
    UiStateHost(state, { viewModel.retry(ProjectSheet.LOCATION) }, sectionModifier) { info ->
        Spacer(Modifier.height(Spacing.xs))
        if (info.description.isNotBlank()) {
            Text(info.description, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyLarge)
        }
        if (info.addressLines.isEmpty()) {
            Text("Endereço não informado.", color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        } else {
            info.addressLines.forEachIndexed { index, line ->
                Text(
                    line,
                    color = if (index == 0) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceMuted,
                    style = if (index == 0) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyMedium,
                )
            }
            ActionRow("Abrir no mapa", LucideR.drawable.lucide_ic_map_pin, { onNavigate(AppDestination.Map) })
            ActionRow("Copiar endereço", LucideR.drawable.lucide_ic_copy, { onCopy(info.copyText) })
        }
    }
}

@Composable
private fun GallerySection(viewModel: ProjectSectionsViewModel) {
    val state by viewModel.photos.collectAsStateWithLifecycle()
    UiStateHost(state, { viewModel.retry(ProjectSheet.GALLERY) }, sectionModifier, "Este projeto ainda não tem imagens.") { photos ->
        Text(
            if (photos.size == 1) "1 imagem" else "${photos.size} imagens",
            color = AppTheme.colors.onSurfaceMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        photos.forEach { photo ->
            ProjectPhotoImage(
                photoId = photo.id,
                load = viewModel::photoContent,
                description = if (photo.cover) "Capa do projeto" else "Imagem do projeto",
                modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(Radius.large)),
            )
        }
    }
}

@Composable
private fun AgendaSection(viewModel: ProjectSectionsViewModel) {
    val state by viewModel.activities.collectAsStateWithLifecycle()
    UiStateHost(state, { viewModel.retry(ProjectSheet.AGENDA) }, sectionModifier, "Nenhuma atividade agendada.") { activities ->
        SectionHeading("Atividades do projeto", faded = true)
        activities.forEach { activity ->
            ActionRow(
                activity.title,
                LucideR.drawable.lucide_ic_calendar_days,
                {},
                trailing = activity.time.ifBlank { null },
                subtitle = activity.subtitle.ifBlank { null },
            )
        }
    }
}

@Composable
private fun ConversationsSection(onNavigate: (AppDestination) -> Unit) {
    Text(
        "As conversas deste projeto ainda não estão disponíveis no servidor. Veja todas as suas conversas abaixo.",
        color = AppTheme.colors.onSurfaceMuted,
        style = MaterialTheme.typography.bodyMedium,
    )
    ActionRow("Ver todas as conversas", LucideR.drawable.lucide_ic_messages_square, { onNavigate(AppDestination.Chats) })
}

@Composable
private fun HistorySection(viewModel: ProjectSectionsViewModel) {
    val state by viewModel.history.collectAsStateWithLifecycle()
    UiStateHost(state, { viewModel.retry(ProjectSheet.HISTORY) }, sectionModifier, "Nada no histórico ainda.") { entries ->
        SectionHeading("Atividade recente", faded = true)
        entries.forEach { TimelineEntry(it.title, it.detail) }
    }
}

@Composable
private fun TeamSection(viewModel: ProjectSectionsViewModel, onNavigate: (AppDestination) -> Unit) {
    val state by viewModel.members.collectAsStateWithLifecycle()
    UiStateHost(state, { viewModel.retry(ProjectSheet.TEAM) }, sectionModifier, "Nenhuma pessoa neste projeto.") { members ->
        SectionHeading("Pessoas envolvidas", faded = true, action = "Ver conexões", onAction = { onNavigate(AppDestination.Connections) })
        members.forEachIndexed { index, member -> TeamEntry(member.initials, member.name, member.role, index) }
    }
}

@Composable
internal fun ProjectPhotoImage(
    photoId: String,
    load: suspend (String) -> ByteArray?,
    description: String,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit = {},
) {
    val image by produceState<ImageBitmap?>(null, photoId) {
        value = load(photoId)?.let { bytes -> withContext(Dispatchers.Default) { decodeSampled(bytes) } }
    }
    val bitmap = image
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = description, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier = modifier.background(AppTheme.colors.onSurfaceMuted.copy(alpha = 0.12f))) { fallback() }
    }
}

private const val MAX_EDGE = 1600

internal fun decodeSampled(bytes: ByteArray): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (bounds.outWidth / sample > MAX_EDGE || bounds.outHeight / sample > MAX_EDGE) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
}

@Composable
private fun TimelineEntry(title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.padding(top = 7.dp).size(9.dp).clip(CircleShape).background(AppTheme.colors.secondary))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
            if (detail.isNotBlank()) Text(detail, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TeamEntry(initials: String, name: String, role: String, index: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(initials = initials, color = AvatarPalette(index))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
            Text(role, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
