@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.core.designsystem.components.localizedActivityType
import com.project.solaria_mobile.core.designsystem.components.localizedRole
import com.project.solaria_mobile.core.designsystem.components.localizedStatus
import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.feature.project.domain.ProjectInviteLink
import com.project.solaria_mobile.feature.project.domain.InviteLinkValidity
import com.project.solaria_mobile.feature.project.domain.INVITE_LINK_ROLES
import com.project.solaria_mobile.core.designsystem.components.SegmentedControl
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.navigationBarsPadding
import com.project.solaria_mobile.core.images.shareImage
import com.project.solaria_mobile.core.images.saveImageToGallery
import com.project.solaria_mobile.core.designsystem.components.AppToastHost
import com.project.solaria_mobile.core.designsystem.components.ToastKind
import com.project.solaria_mobile.core.designsystem.components.rememberToastState
import com.project.solaria_mobile.core.designsystem.components.AppDatePickerDialog
import com.project.solaria_mobile.core.designsystem.components.AppDialog
import com.project.solaria_mobile.core.designsystem.components.AppTimePickerDialog
import com.project.solaria_mobile.core.designsystem.components.AppButton
import com.project.solaria_mobile.core.designsystem.components.ButtonVariant
import com.project.solaria_mobile.core.designsystem.components.ButtonSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.components.AvatarPalette
import com.project.solaria_mobile.core.designsystem.components.RemoteImage
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.ContentSkeleton
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.presentation.navigation.AppDestination
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.ContentScale
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.project.solaria_mobile.feature.project.domain.ProjectStatusOption
import com.project.solaria_mobile.core.designsystem.components.StatusBadge
import com.project.solaria_mobile.feature.project.domain.ProjectRoleOption
import com.project.solaria_mobile.core.designsystem.components.FormField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.fillMaxSize
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.feature.project.domain.ProjectPhoto
import com.project.solaria_mobile.feature.project.domain.ActivityDraft
import com.project.solaria_mobile.feature.project.domain.ActivityField
import com.project.solaria_mobile.feature.project.domain.ActivityTypeOption
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.project.solaria_mobile.feature.project.domain.ProjectActivity
import com.project.solaria_mobile.core.images.readPickedImage
import com.project.solaria_mobile.core.images.rememberCameraCapture
import com.project.solaria_mobile.core.designsystem.components.GallerySkeleton

enum class ProjectSheet {
    LOCATION,
    GALLERY,
    AGENDA,
    CONVERSATIONS,
    HISTORY,
    TEAM,
    REPORT,
}

private val sectionModifier = Modifier.fillMaxWidth()

@Composable
internal fun ProjectSheetContent(
    sheet: ProjectSheet,
    viewModel: ProjectSectionsViewModel,
    onNavigate: (AppDestination) -> Unit,
    onCopy: (String) -> Unit,
    onOpenDetail: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.sm, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        when (sheet) {
            ProjectSheet.LOCATION -> LocationSection(viewModel, onNavigate, onCopy)
            ProjectSheet.GALLERY -> GallerySection(viewModel) { onOpenDetail(PHOTO_DETAIL + it) }
            ProjectSheet.AGENDA -> AgendaSection(viewModel) { onOpenDetail(if (it == NEW_ACTIVITY) ACTIVITY_FORM + NEW_ACTIVITY else ACTIVITY_DETAIL + it) }
            ProjectSheet.CONVERSATIONS -> com.project.solaria_mobile.feature.communication.ProjectCommunitySection(onNavigate)
            ProjectSheet.HISTORY -> HistorySection(viewModel, onOpenDetail)
            ProjectSheet.REPORT -> ReportForm(viewModel)
            ProjectSheet.TEAM -> TeamSection(viewModel, onNavigate, onOpenDetail)
        }
    }
}

/** Segundo nível de uma seção, numa sheet empilhada sobre a primeira; fechar devolve para a lista por baixo. */
@Composable
internal fun SheetDetailContent(detail: String, viewModel: ProjectSectionsViewModel, onNavigate: (AppDestination) -> Unit, onOpenDetail: (String) -> Unit) {
    val dismiss = LocalSheetDismiss.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SheetDetail(detail, viewModel, onNavigate, dismiss, onOpenDetail)
    }
}

@Composable
private fun LocationSection(viewModel: ProjectSectionsViewModel, onNavigate: (AppDestination) -> Unit, onCopy: (String) -> Unit) {
    val state by viewModel.location.collectAsStateWithLifecycle()
    UiStateHost(state, { viewModel.retry(ProjectSheet.LOCATION) }, sectionModifier, inline = true, loading = { ContentSkeleton() }) { info ->
        val context = LocalContext.current
        if (info.addressLines.isEmpty()) {
            Text(stringResource(R.string.ps_endereco_nao_informado), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        } else {
            Column {
                // A primeira linha começa rente ao topo da sheet, como as demais sheets do projeto.
                info.fields.forEachIndexed { index, field ->
                    com.project.solaria_mobile.feature.profile.ProfileField(field.label, field.value, verticalPadding = Spacing.action, topPadding = if (index == 0) 0.dp else Spacing.action)
                }
            }
            // `geo:` deixa o Android listar os apps de mapas instalados (Google Maps, Waze…) em vez de forçar um só.
            val openWith = stringResource(R.string.ps_abrir_com)
            ActionRow(stringResource(R.string.ps_abrir_no_maps), AppIcons.map_pin, {
                val target = if (info.latitude != null && info.longitude != null) "${info.latitude},${info.longitude}" else null
                val uri = android.net.Uri.parse(if (target != null) "geo:$target?q=$target" else "geo:0,0?q=${android.net.Uri.encode(info.copyText)}")
                try { context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_VIEW, uri), openWith)) }
                catch (_: android.content.ActivityNotFoundException) { }
            })
            ActionRow(stringResource(R.string.ps_ver_no_mapa_do_app), AppIcons.map_pin, { onNavigate(AppDestination.Map(viewModel.projectId)) })
            ActionRow(stringResource(R.string.ps_copiar_endereco), AppIcons.copy, { onCopy(info.copyText) })
        }
    }
}

@Composable
private fun GallerySection(viewModel: ProjectSectionsViewModel, onOpenPhoto: (String) -> Unit) {
    val state by viewModel.photos.collectAsStateWithLifecycle()
    val action by viewModel.photoAction.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val upload: (android.net.Uri?) -> Unit = { uri ->
        if (uri != null) scope.launch {
            val picked = withContext(Dispatchers.IO) { readPickedImage(context, uri) }
            if (picked == null) viewModel.rejectPhoto() else viewModel.uploadPhoto(picked.first, picked.second)
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), upload)
    val capture = rememberCameraCapture(upload)
    ActionRow(stringResource(R.string.ps_escolher_da_galeria), AppIcons.images, {
        viewModel.clearPhotoAction()
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }, compact = true)
    ActionRow(stringResource(R.string.ps_tirar_foto), AppIcons.camera, {
        viewModel.clearPhotoAction()
        capture()
    }, compact = true)
    ProjectActionStatus(action, working = stringResource(R.string.ps_enviando_imagem))
    var gridMode by rememberSaveable { mutableStateOf(false) }
    var coverMode by rememberSaveable { mutableStateOf(false) }
    UiStateHost(state, { viewModel.retry(ProjectSheet.GALLERY) }, sectionModifier, inline = true, emptyMessage = stringResource(R.string.ps_este_projeto_ainda_nao_tem), loading = { GallerySkeleton() }) { photos ->
        val colors = AppTheme.colors
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (photos.size == 1) stringResource(R.string.ps_1_imagem) else stringResource(R.string.ps_n_imagens, photos.size),
                color = colors.onSurfaceMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                RoundIconButton(
                    AppIcons.cover, if (coverMode) stringResource(R.string.ps_cancelar_a_escolha_da_capa) else stringResource(R.string.ps_escolher_a_capa_do_projeto),
                    { viewModel.clearPhotoAction(); coverMode = !coverMode },
                    background = if (coverMode) colors.action else colors.inputFill,
                    tint = if (coverMode) colors.onAccent else colors.onSurface,
                )
                RoundIconButton(
                    if (gridMode) AppIcons.layout_list else AppIcons.layout_grid,
                    if (gridMode) stringResource(R.string.ps_mostrar_lista) else stringResource(R.string.ps_mostrar_grade),
                    { gridMode = !gridMode },
                )
            }
        }
        if (coverMode) {
            Text(stringResource(R.string.ps_toque_em_uma_imagem_para), color = colors.onSurface, style = MaterialTheme.typography.titleSmall)
        }
        val choose: (String) -> Unit = { id ->
            if (coverMode) { viewModel.clearPhotoAction(); viewModel.setCover(id); coverMode = false } else onOpenPhoto(id)
        }
        if (gridMode) {
            photos.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    pair.forEach { photo -> GalleryPhoto(photo, viewModel, Modifier.weight(1f).aspectRatio(1f)) { choose(photo.id) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            photos.forEach { photo -> GalleryPhoto(photo, viewModel, Modifier.fillMaxWidth().height(Sizing.coverHeight)) { choose(photo.id) } }
        }
    }
}

@Composable
private fun GalleryPhoto(photo: ProjectPhoto, viewModel: ProjectSectionsViewModel, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(Radius.large)).clickable(onClick = onClick)) {
        RemoteImage(
            key = photo.id,
            load = viewModel::photoContent,
            description = if (photo.cover) stringResource(R.string.ps_capa_do_projeto) else stringResource(R.string.ps_imagem_do_projeto),
            modifier = Modifier.fillMaxSize(),
        )
        if (photo.cover) {
            Text(
                stringResource(R.string.ps_capa),
                modifier = Modifier.align(Alignment.TopStart).padding(Spacing.xs)
                    .clip(RoundedCornerShape(Radius.pill)).background(AppTheme.colors.surface.copy(alpha = 0.9f))
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                color = AppTheme.colors.onSurface,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * Imagem numa sheet própria, empilhada sobre a galeria: texto à esquerda e ações à direita; a imagem ocupa toda a
 * largura e, tocando nela, abre em tela cheia com zoom.
 */
@Composable
internal fun PhotoSheetContent(photoId: String, viewModel: ProjectSectionsViewModel) {
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val photo = (photos as? UiState.Content)?.data?.firstOrNull { it.id == photoId }
    val photoAction by viewModel.photoAction.collectAsStateWithLifecycle()
    val dismiss = LocalSheetDismiss.current
    var confirmRemoval by remember(photoId) { mutableStateOf(false) }
    var viewing by remember(photoId) { mutableStateOf(false) }
    // Depois de remover, a foto sai da galeria recarregada e a sheet da imagem se fecha sozinha.
    LaunchedEffect(photo, photoAction) { if (photo == null && photos is UiState.Content && photoAction == null) dismiss() }
    // Se a remoção falhar, o popup fecha e o erro aparece na própria sheet.
    LaunchedEffect(photoAction) { if (photoAction is ProjectAction.Failed) confirmRemoval = false }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (photo == null) {
            StateNotice(stringResource(R.string.ps_imagem_indisponivel), "Esta imagem não está mais no projeto.", icon = AppIcons.images)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (photo.cover) stringResource(R.string.ps_capa_do_projeto) else stringResource(R.string.ps_imagem_do_projeto),
                    modifier = Modifier.weight(1f),
                    color = AppTheme.colors.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    RoundIconButton(
                        AppIcons.cover, if (photo.cover) stringResource(R.string.ps_esta_e_a_capa_do) else stringResource(R.string.ps_definir_como_capa),
                        { viewModel.clearPhotoAction(); viewModel.setCover(photo.id) },
                        enabled = !photo.cover,
                        background = if (photo.cover) AppTheme.colors.action else AppTheme.colors.inputFill,
                        tint = if (photo.cover) AppTheme.colors.onAccent else AppTheme.colors.onSurface,
                    )
                    RoundIconButton(AppIcons.trash, stringResource(R.string.ps_remover_imagem), { viewModel.clearPhotoAction(); confirmRemoval = true })
                }
            }
            RemoteImage(
                key = photo.id,
                load = viewModel::photoContent,
                description = if (photo.cover) stringResource(R.string.ps_capa_do_projeto) else stringResource(R.string.ps_imagem_do_projeto),
                modifier = Modifier.fillMaxWidth().aspectRatio(PHOTO_ASPECT).clip(RoundedCornerShape(Radius.large))
                    .clickable { viewing = true },
            )
            ProjectActionStatus(photoAction, working = stringResource(R.string.ps_salvando))
            if (confirmRemoval) AppDialog(
                onDismissRequest = { confirmRemoval = false },
                title = stringResource(R.string.ps_remover_esta_imagem),
                message = stringResource(R.string.ps_a_imagem_saira_do_projeto),
                confirmLabel = stringResource(R.string.ps_remover),
                onConfirm = { viewModel.removePhoto(photo.id) },
                destructive = true,
                loading = photoAction == ProjectAction.Working,
            )
            if (viewing) PhotoViewer(photo.id, viewModel) { viewing = false }
        }
    }
}

/** Imagem em tela cheia, sobre fundo preto, com zoom por pinça e arraste; o toque (sem zoom) ou o voltar fecham. */
@Composable
private fun PhotoViewer(photoId: String, viewModel: ProjectSectionsViewModel, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val feedback = rememberToastState()
        val loadFailed = stringResource(R.string.ps_nao_foi_possivel_carregar_a)
        val shareChoose = stringResource(R.string.ps_escolha_onde_compartilhar)
        val shareFailed = stringResource(R.string.ps_nao_foi_possivel_compartilhar_a)
        val saveDone = stringResource(R.string.ps_imagem_salva_na_galeria)
        val saveFailed = stringResource(R.string.ps_nao_foi_possivel_salvar_a)
        var busy by remember { mutableStateOf(false) }
        val fileName = "imagem-${photoId.takeLast(8)}"
        // Baixa e compartilha usam os bytes já carregados pela sheet; o aviso aparece sobre a imagem.
        val run: (suspend (ByteArray) -> Pair<String, ToastKind>) -> Unit = { action ->
            if (!busy) scope.launch {
                busy = true
                val bytes = viewModel.photoContent(photoId)
                val (text, kind) = if (bytes == null) loadFailed to ToastKind.Error else action(bytes)
                feedback.show(text, kind)
                busy = false
            }
        }
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
            RemoteImage(
                key = photoId,
                load = viewModel::photoContent,
                description = stringResource(R.string.ps_imagem_do_projeto_em_tela),
                modifier = Modifier.fillMaxSize()
                    .zoomAndTap(scale, { scale = it }, offset, { offset = it }, onClose)
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
                contentScale = ContentScale.Fit,
                placeholder = false,
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                RoundIconButton(AppIcons.share, stringResource(R.string.ps_compartilhar_imagem), {
                    run { bytes -> if (shareImage(context, bytes, fileName)) shareChoose to ToastKind.Info else shareFailed to ToastKind.Error }
                }, enabled = !busy)
                RoundIconButton(AppIcons.download, stringResource(R.string.ps_baixar_imagem), {
                    run { bytes -> if (saveImageToGallery(context, bytes, fileName)) saveDone to ToastKind.Success else saveFailed to ToastKind.Error }
                }, enabled = !busy)
                RoundIconButton(AppIcons.x, stringResource(R.string.ps_fechar_imagem), onClose)
            }
            AppToastHost(feedback, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(Spacing.md))
        }
    }
}

private fun Modifier.zoomAndTap(
    scale: Float, onScale: (Float) -> Unit,
    offset: androidx.compose.ui.geometry.Offset, onOffset: (androidx.compose.ui.geometry.Offset) -> Unit,
    onTap: () -> Unit,
): Modifier = this
    .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            val next = (scale * zoom).coerceIn(1f, 4f)
            onScale(next)
            onOffset(if (next <= 1f) androidx.compose.ui.geometry.Offset.Zero else offset + pan)
        }
    }
    .pointerInput(scale) { detectTapGestures(onTap = { if (scale <= 1f) onTap() }, onDoubleTap = { onScale(if (scale > 1f) 1f else 2f); onOffset(androidx.compose.ui.geometry.Offset.Zero) }) }

private const val PHOTO_ASPECT = 4f / 3f

/** O formulário de atividade abre numa terceira sheet, empilhada sobre o detalhe. */
internal fun isActivityForm(detail: String): Boolean = detail.startsWith(ACTIVITY_FORM)

internal fun photoIdOf(detail: String?): String? = detail?.takeIf { it.startsWith(PHOTO_DETAIL) }?.removePrefix(PHOTO_DETAIL)

@Composable
private fun AgendaSection(viewModel: ProjectSectionsViewModel, onOpenActivity: (String) -> Unit) {
    val state by viewModel.activities.collectAsStateWithLifecycle()
    ActionRow(stringResource(R.string.ps_nova_atividade), AppIcons.calendar_days, { viewModel.clearActivityAction(); onOpenActivity(NEW_ACTIVITY) }, compact = true)
    UiStateHost(state, { viewModel.retry(ProjectSheet.AGENDA) }, sectionModifier, inline = true, emptyMessage = stringResource(R.string.ps_nenhuma_atividade_agendada), loading = { ListSkeleton(rows = 3, circle = false) }) { activities ->
        SectionHeading(stringResource(R.string.ps_atividades_do_projeto), faded = true)
        activities.forEach { activity ->
            ActionRow(
                activity.title,
                AppIcons.calendar_days,
                { onOpenActivity(activity.id) },
                trailing = activity.time.ifBlank { null },
                subtitle = activity.subtitle.ifBlank { null },
            )
        }
    }
}

@Composable
private fun HistorySection(viewModel: ProjectSectionsViewModel, onOpenDetail: (String) -> Unit) {
    val state by viewModel.history.collectAsStateWithLifecycle()
    UiStateHost(state, { viewModel.retry(ProjectSheet.HISTORY) }, sectionModifier, inline = true, emptyMessage = stringResource(R.string.ps_nada_no_historico_ainda), loading = { ListSkeleton(rows = 4, leading = Spacing.md) }) { entries ->
        SectionHeading(stringResource(R.string.ps_atividade_recente), faded = true)
        entries.forEach { entry -> TimelineEntry(entry.title, entry.detail, entry.reportId?.let { id -> { onOpenDetail(REPORT_DETAIL + id) } }) }
    }
}

@Composable
private fun TeamSection(viewModel: ProjectSectionsViewModel, onNavigate: (AppDestination) -> Unit, onOpenDetail: (String) -> Unit) {
    val state by viewModel.members.collectAsStateWithLifecycle()
    val invitations by viewModel.invitations.collectAsStateWithLifecycle()
    ActionRow(stringResource(R.string.ps_convidar_pessoa), AppIcons.user_round_plus, { viewModel.clearTeamAction(); onOpenDetail(INVITE_DETAIL) }, compact = true)
    ActionRow(stringResource(R.string.ps_convidar_por_link), AppIcons.link, { viewModel.clearTeamAction(); onOpenDetail(INVITE_LINK_DETAIL) }, compact = true)
    ActionRow(stringResource(R.string.ps_minhas_conexoes), AppIcons.users, { onNavigate(AppDestination.Connections) }, compact = true)
    UiStateHost(state, { viewModel.retry(ProjectSheet.TEAM) }, sectionModifier, inline = true, emptyMessage = stringResource(R.string.ps_nenhuma_pessoa_neste_projeto), loading = { ListSkeleton(rows = 4) }) { members ->
        SectionHeading(stringResource(R.string.ps_pessoas_envolvidas), faded = true)
        members.forEachIndexed { index, member -> TeamEntry(member.initials, member.name, member.role, index) { onOpenDetail(MEMBER_DETAIL + member.id) } }
        if (invitations.isNotEmpty()) {
            SectionHeading(stringResource(R.string.ps_convites_pendentes), faded = true)
            invitations.forEach { invitation ->
                ActionRow(invitation.email, AppIcons.mail, { viewModel.clearTeamAction(); onOpenDetail(INVITATION_DETAIL + invitation.id) },
                    subtitle = listOf(invitation.role, invitation.expires).filter { it.isNotBlank() }.joinToString(" · "), compact = true)
            }
        }
    }
}

@Composable
private fun TimelineEntry(title: String, detail: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.medium))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.padding(top = 7.dp).size(9.dp).clip(CircleShape).background(AppTheme.colors.onSurface))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
            if (detail.isNotBlank()) Text(detail, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        }
        if (onClick != null) Icon(painterResource(AppIcons.chevron_right), null, Modifier.size(Sizing.iconSmall), tint = AppTheme.colors.onSurfaceMuted)
    }
}

@Composable
private fun TeamEntry(initials: String, name: String, role: String, index: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.medium)).clickable(onClick = onClick).padding(vertical = Spacing.xs),
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

internal const val PHOTO_DETAIL = "photo:"
private const val REPORT_DETAIL = "report:"
private const val MEMBER_DETAIL = "member:"
private const val ACTIVITY_DETAIL = "activity:"
private const val INVITE_DETAIL = "invite"
private const val INVITE_LINK_DETAIL = "invite-link"
private const val NEW_ACTIVITY = "new"
private const val ACTIVITY_FORM = "activity-form:"
private const val INVITATION_DETAIL = "invitation:"

/** Segundo nível de uma seção: foto da galeria ou pessoa dos envolvidos, com retorno ao nível anterior. */
@Composable
private fun SheetDetail(detail: String, viewModel: ProjectSectionsViewModel, onNavigate: (AppDestination) -> Unit, onClose: () -> Unit, onOpenDetail: (String) -> Unit) {
    when {
        detail.startsWith(ACTIVITY_FORM) -> {
            val activities by viewModel.activities.collectAsStateWithLifecycle()
            val editing = detail.removePrefix(ACTIVITY_FORM).takeIf { it != NEW_ACTIVITY }
            val activity = editing?.let { id -> (activities as? UiState.Content)?.data?.firstOrNull { it.id == id } }
            if (editing != null && activity == null) {
                StateNotice(stringResource(R.string.ps_atividade_indisponivel), "Esta atividade não está mais no projeto.", icon = AppIcons.calendar_days)
            } else {
                ActivityForm(activity, viewModel, onClose)
            }
        }
        detail.startsWith(REPORT_DETAIL) -> ReportDetail(detail.removePrefix(REPORT_DETAIL), viewModel) { photoId -> onOpenDetail(PHOTO_DETAIL + photoId) }
        detail == INVITE_LINK_DETAIL -> InviteLinkSheet(viewModel)
        detail == INVITE_DETAIL -> {
            InviteForm(viewModel, onClose)
        }
        detail.startsWith(INVITATION_DETAIL) -> {
            val invitations by viewModel.invitations.collectAsStateWithLifecycle()
            val invitation = invitations.firstOrNull { it.id == detail.removePrefix(INVITATION_DETAIL) }
            val action by viewModel.teamAction.collectAsStateWithLifecycle()
            LaunchedEffect(action) { if ((action as? ProjectAction.Done)?.operation == ProjectOperation.REVOKE_INVITE) onClose() }
            if (invitation == null) {
                StateNotice(stringResource(R.string.ps_convite_indisponivel), "Este convite já foi respondido ou cancelado.", icon = AppIcons.mail)
            } else {
                Text(invitation.email, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
                Text(listOf(invitation.role, invitation.expires).filter { it.isNotBlank() }.joinToString(" · "),
                    color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                ActionRow(stringResource(R.string.ps_reenviar_convite), AppIcons.mail, { viewModel.clearTeamAction(); viewModel.resendInvitation(invitation.id) }, compact = true)
                ActionRow(stringResource(R.string.ps_cancelar_convite), AppIcons.users, { viewModel.clearTeamAction(); viewModel.revokeInvitation(invitation.id) }, compact = true)
                if ((action as? ProjectAction.Done)?.operation == ProjectOperation.RESEND_INVITE) {
                    Text(stringResource(R.string.ps_convite_reenviado), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                }
                ProjectActionStatus(action, working = stringResource(R.string.ps_salvando))
            }
        }
        detail.startsWith(ACTIVITY_DETAIL) -> {
            val activities by viewModel.activities.collectAsStateWithLifecycle()
            val activity = (activities as? UiState.Content)?.data?.firstOrNull { it.id == detail.removePrefix(ACTIVITY_DETAIL) }
            val action by viewModel.activityAction.collectAsStateWithLifecycle()
            if (activity == null) {
                StateNotice(stringResource(R.string.ps_atividade_indisponivel), "Esta atividade não está mais no projeto.", icon = AppIcons.calendar_days)
            } else {
                ActivityDetail(activity, action, viewModel) { onOpenDetail(ACTIVITY_FORM + activity.id) }
            }
        }
        detail.startsWith(MEMBER_DETAIL) -> {
            val members by viewModel.members.collectAsStateWithLifecycle()
            val list = (members as? UiState.Content)?.data.orEmpty()
            val index = list.indexOfFirst { it.id == detail.removePrefix(MEMBER_DETAIL) }
            val member = list.getOrNull(index)
            if (member == null) {
                StateNotice(stringResource(R.string.ps_pessoa_indisponivel), "Esta pessoa não participa mais do projeto.", icon = AppIcons.users)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Avatar(initials = member.initials, size = Sizing.avatarLarge, color = AvatarPalette(index))
                    Column {
                        Text(member.name, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
                        Text(member.role, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                ActionRow(stringResource(R.string.ps_ver_perfil), AppIcons.user_round_check, { onNavigate(AppDestination.ProfileOther(member.userId)) })
                if (!member.owner) RemoveMember(member.userId, viewModel, onClose)
            }
        }
    }
}


@Composable
private fun RemoveMember(userId: String, viewModel: ProjectSectionsViewModel, onRemoved: () -> Unit) {
    val action by viewModel.teamAction.collectAsStateWithLifecycle()
    var confirm by remember(userId) { mutableStateOf(false) }
    LaunchedEffect(action) { if ((action as? ProjectAction.Done)?.operation == ProjectOperation.REMOVE_MEMBER) onRemoved() }
    // A confirmação é um popup, não uma troca do conteúdo da sheet.
    LaunchedEffect(action) { if (action is ProjectAction.Failed) confirm = false }
    if (confirm) AppDialog(
        onDismissRequest = { confirm = false },
        title = stringResource(R.string.ps_remover_esta_pessoa),
        message = stringResource(R.string.ps_ela_deixa_de_ver_e),
        confirmLabel = stringResource(R.string.ps_remover),
        onConfirm = { viewModel.removeMember(userId) },
        destructive = true,
        loading = action == ProjectAction.Working,
    )
    ActionRow(stringResource(R.string.ps_remover_do_projeto), AppIcons.users, { viewModel.clearTeamAction(); confirm = true }, compact = true)
    ProjectActionStatus(action, working = stringResource(R.string.ps_salvando))
}

/** Convite por e-mail com papel e função; o e-mail é conferido no aparelho antes de chamar o servidor. */
@Composable
private fun InviteForm(viewModel: ProjectSectionsViewModel, onSent: () -> Unit) {
    val action by viewModel.teamAction.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(ProjectRoleOption.MEMBER) }
    var functionName by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(action) { if ((action as? ProjectAction.Done)?.operation == ProjectOperation.INVITE) onSent() }
    Text(stringResource(R.string.ps_convidar_pessoa), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
    FormField(label = stringResource(R.string.ps_e_mail), value = email, onValueChange = { email = it }, keyboardType = KeyboardType.Email)
    Text(stringResource(R.string.ps_papel_no_projeto), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
    ProjectRoleOption.entries.forEach { option ->
        ActionRow(localizedRole(option.label), if (option == role) AppIcons.check else AppIcons.users, { role = option }, trailing = if (option == role) stringResource(R.string.journey_preset_selected) else null, compact = true)
    }
    FormField(label = stringResource(R.string.ps_funcao_opcional), value = functionName, onValueChange = { if (it.length <= MAX_FUNCTION_LENGTH) functionName = it })
    ProjectActionStatus(action, working = stringResource(R.string.ps_enviando_convite))
    PrimaryButton(stringResource(R.string.ps_enviar_convite), {
        if (EMAIL_PATTERN.matches(email.trim())) viewModel.invite(email, role, functionName) else viewModel.rejectInvite()
    }, enabled = action != ProjectAction.Working && email.isNotBlank())
}

/**
 * Convite por link: papel e validade, criação e, só logo depois dela, o endereço para copiar ou compartilhar. A lista
 * abaixo mostra os links ativos sem o endereço (o servidor não o guarda) e permite cancelar.
 */
@Composable
private fun InviteLinkSheet(viewModel: ProjectSectionsViewModel) {
    val action by viewModel.teamAction.collectAsStateWithLifecycle()
    val links by viewModel.inviteLinks.collectAsStateWithLifecycle()
    val created by viewModel.createdLink.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { viewModel.clearCreatedLink() } }
    var role by rememberSaveable { mutableStateOf(0) }
    var validity by rememberSaveable { mutableStateOf(1) }
    var copied by remember(created?.id) { mutableStateOf(false) }
    var revoking by remember { mutableStateOf<ProjectInviteLink?>(null) }
    val validities = InviteLinkValidity.entries

    Text(stringResource(R.string.ps_convidar_por_link), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
    Text(
        stringResource(R.string.ps_quem_tiver_o_link_entra),
        color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium,
    )
    Text(stringResource(R.string.ps_papel_no_projeto), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
    SegmentedControl(INVITE_LINK_ROLES.map { it.label }, role, { role = it; viewModel.clearTeamAction() })
    Text(stringResource(R.string.ps_validade), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
    SegmentedControl(validities.map { it.label }, validity, { validity = it; viewModel.clearTeamAction() })
    ProjectActionStatus(action, working = stringResource(R.string.ps_criando_o_link))
    PrimaryButton(stringResource(R.string.ps_criar_link), {
        viewModel.clearTeamAction()
        viewModel.createInviteLink(INVITE_LINK_ROLES[role], validities[validity])
    }, enabled = action != ProjectAction.Working)

    created?.url?.let { url ->
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.medium)).background(com.project.solaria_mobile.core.designsystem.theme.containerFill()).padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(url, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.ps_este_link_so_aparece_agora), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        }
        val shareLinkTitle = stringResource(R.string.ps_compartilhar_link)
        val joinText = stringResource(R.string.ps_entre_no_projeto, url)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AppButton(
                if (copied) stringResource(R.string.ps_copiado) else stringResource(R.string.ps_copiar), { clipboard.setText(androidx.compose.ui.text.AnnotatedString(url)); copied = true },
                Modifier.weight(1f).fillMaxHeight(), ButtonVariant.Secondary, ButtonSize.Medium, fillWidth = false,
            )
            AppButton(
                stringResource(R.string.ps_compartilhar), {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(android.content.Intent.EXTRA_TEXT, joinText)
                    try { context.startActivity(android.content.Intent.createChooser(send, shareLinkTitle)) } catch (_: android.content.ActivityNotFoundException) { }
                },
                Modifier.weight(1f).fillMaxHeight(), ButtonVariant.Primary, ButtonSize.Medium, fillWidth = false,
            )
        }
    }

    if (links.isNotEmpty()) {
        SectionHeading(stringResource(R.string.ps_links_ativos), faded = true)
        links.forEach { link ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Text(link.role, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOf(link.expires, if (link.uses == 1) stringResource(R.string.ps_1_entrada) else stringResource(R.string.ps_n_entradas, link.uses)).filter { it.isNotBlank() }.joinToString(" · "),
                        color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium,
                    )
                }
                RoundIconButton(AppIcons.trash, stringResource(R.string.ps_desativar_link), { viewModel.clearTeamAction(); revoking = link })
            }
        }
    }
    revoking?.let { link ->
        AppDialog(
            onDismissRequest = { revoking = null },
            title = stringResource(R.string.ps_desativar_este_link),
            message = stringResource(R.string.ps_quem_ainda_nao_entrou_nao),
            confirmLabel = stringResource(R.string.ps_desativar),
            dismissLabel = stringResource(R.string.common_back),
            onConfirm = { viewModel.revokeInviteLink(link.id); revoking = null },
            destructive = true,
            loading = action == ProjectAction.Working,
        )
    }
}

private const val MAX_FUNCTION_LENGTH = 120
private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Formulário de atividade: data e horários pelos seletores do sistema; o tipo não muda na edição, como no api-core. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityForm(activity: ProjectActivity?, viewModel: ProjectSectionsViewModel, onSaved: () -> Unit) {
    val action by viewModel.activityAction.collectAsStateWithLifecycle()
    val membersState by viewModel.members.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.open(ProjectSheet.TEAM) }
    LaunchedEffect(action) { if ((action as? ProjectAction.Done)?.operation == ProjectOperation.SAVE_ACTIVITY) onSaved() }
    var draft by remember(activity?.id) { mutableStateOf(activity?.let(ActivityDraft::from) ?: ActivityDraft()) }
    var picking by remember { mutableStateOf<ActivityPicker?>(null) }
    val errors = if ((action as? ProjectAction.Failed)?.kind == com.project.solaria_mobile.core.common.ErrorKind.VALIDATION) draft.validate() else emptySet()
    val update: (ActivityDraft) -> Unit = { viewModel.clearActivityAction(); draft = it }

    Text(if (activity == null) stringResource(R.string.ps_nova_atividade) else stringResource(R.string.ps_editar_atividade), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
    FormField(label = stringResource(R.string.ps_titulo), value = draft.title, onValueChange = { update(draft.copy(title = it)) }, maxLength = 160,
        error = if (ActivityField.TITLE in errors) stringResource(R.string.ps_informe_um_titulo_de_ate) else null)
    FormField(label = stringResource(R.string.ps_descricao_opcional), value = draft.description, onValueChange = { update(draft.copy(description = it)) },
        singleLine = false, minLines = 2, maxLength = 4000)
    Text(stringResource(R.string.ps_tipo), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
    if (activity == null) {
        ActivityTypeOption.entries.forEach { option ->
            ActionRow(localizedActivityType(option.label), if (option == draft.type) AppIcons.check else option.icon, { update(draft.copy(type = option)) },
                trailing = if (option == draft.type) stringResource(R.string.journey_preset_selected) else null, compact = true)
        }
    } else {
        Text(localizedActivityType(draft.type.label), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
    }
    ActionRow(stringResource(R.string.ps_data), AppIcons.calendar_days, { picking = ActivityPicker.DATE }, trailing = draft.date.format(DATE_LABEL), compact = true)
    ActionRow(stringResource(R.string.time_start), AppIcons.clock, { picking = ActivityPicker.START }, trailing = draft.start.format(TIME_LABEL), compact = true)
    ActionRow(stringResource(R.string.ps_termino), AppIcons.clock, { picking = ActivityPicker.END }, trailing = draft.end.format(TIME_LABEL), compact = true)
    if (ActivityField.PERIOD in errors) Text(stringResource(R.string.ps_o_termino_precisa_ser_depois), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
    ActionRow(stringResource(R.string.ps_no_local_do_projeto), if (draft.onsite) AppIcons.check else AppIcons.map_pin, { update(draft.copy(onsite = !draft.onsite)) },
        trailing = if (draft.onsite) stringResource(R.string.ps_sim) else stringResource(R.string.ps_nao), compact = true)
    val members = (membersState as? UiState.Content)?.data.orEmpty().filterNot { it.viewer }
    if (members.isNotEmpty()) {
        Text(stringResource(R.string.ps_responsaveis), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
        members.forEach { member ->
            val chosen = member.userId in draft.assigneeIds
            ActionRow(member.name, if (chosen) AppIcons.check else AppIcons.users, {
                update(draft.copy(assigneeIds = if (chosen) draft.assigneeIds - member.userId else draft.assigneeIds + member.userId))
            }, subtitle = member.role, trailing = if (chosen) stringResource(R.string.journey_preset_selected) else null, compact = true)
        }
    }
    ProjectActionStatus(action, working = stringResource(R.string.ps_salvando))
    PrimaryButton(if (activity == null) stringResource(R.string.ps_criar_atividade) else stringResource(R.string.ps_salvar_atividade), { viewModel.saveActivity(activity?.id, draft) },
        enabled = action != ProjectAction.Working)

    when (picking) {
        ActivityPicker.DATE -> AppDatePickerDialog(
            initial = draft.date,
            onConfirm = { day -> update(draft.copy(date = day)); picking = null },
            onDismiss = { picking = null },
        )
        ActivityPicker.START, ActivityPicker.END -> AppTimePickerDialog(
            initial = if (picking == ActivityPicker.START) draft.start else draft.end,
            onConfirm = { time -> update(if (picking == ActivityPicker.START) draft.copy(start = time) else draft.copy(end = time)); picking = null },
            onDismiss = { picking = null },
        )
        null -> Unit
    }
}

private enum class ActivityPicker { DATE, START, END }

private val DETAIL_DATE = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))

private fun statusIcon(option: ProjectStatusOption): Int = when (option) {
    ProjectStatusOption.OPEN -> AppIcons.circle
    ProjectStatusOption.IN_PROGRESS -> AppIcons.clock
    ProjectStatusOption.COMPLETED -> AppIcons.check
    ProjectStatusOption.CANCELED -> AppIcons.x
}

/** Atividade aberta: título, tipo e situação à esquerda, edição e troca de situação à direita; depois os dados e a descrição. */
@Composable
private fun ActivityDetail(activity: ProjectActivity, action: ProjectAction?, viewModel: ProjectSectionsViewModel, onEdit: () -> Unit) {
    val membersState by viewModel.members.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.open(ProjectSheet.TEAM) }
    val zone = java.time.ZoneId.systemDefault()
    val start = activity.startsAt?.atZoneSameInstant(zone)
    val end = activity.endsAt?.atZoneSameInstant(zone)
    val assignees = (membersState as? UiState.Content)?.data.orEmpty().filter { it.userId in activity.assigneeIds }.joinToString(", ") { it.name }

    // O título ocupa a largura toda e a linha de baixo tem sempre a altura do botão; trocar a situação não muda a altura da sheet.
    Text(activity.title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.headlineMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            activity.type?.let { StatusBadge(it.label) }
            activity.status?.let { StatusBadge(it.label, tint = projectStatusTint(it.label)) }
        }
        // Um só botão: o ícone mostra a situação atual e cada toque passa para a próxima.
        activity.status?.let { current ->
            val next = ProjectStatusOption.entries[(current.ordinal + 1) % ProjectStatusOption.entries.size]
            val tint = projectStatusTint(current.label)
            RoundIconButton(
                statusIcon(current), stringResource(R.string.ps_situacao_toque, localizedStatus(current.label), localizedStatus(next.label)),
                { viewModel.clearActivityAction(); viewModel.changeActivityStatus(activity.id, next) },
                tint = tint, background = tint.copy(alpha = 0.12f), enabled = action != ProjectAction.Working,
            )
        }
    }
    ProjectActionStatus(action, working = stringResource(R.string.ps_salvando))
    SectionHeading(stringResource(R.string.ps_detalhes), faded = true)
    Column {
        if (start != null) com.project.solaria_mobile.feature.profile.ProfileField(stringResource(R.string.ps_data), start.format(DETAIL_DATE), Spacing.action)
        if (start != null) com.project.solaria_mobile.feature.profile.ProfileField(
            stringResource(R.string.time_title), if (end != null) "${start.format(TIME_LABEL)} – ${end.format(TIME_LABEL)}" else start.format(TIME_LABEL), Spacing.action,
        )
        com.project.solaria_mobile.feature.profile.ProfileField(stringResource(R.string.onboarding_step_place), if (activity.onsite) stringResource(R.string.ps_no_local_do_projeto) else stringResource(R.string.ps_fora_do_local), Spacing.action)
        if (assignees.isNotBlank()) com.project.solaria_mobile.feature.profile.ProfileField(stringResource(R.string.ps_responsaveis), assignees, Spacing.action)
    }
    if (activity.description.isNotBlank()) {
        SectionHeading(stringResource(R.string.ps_descricao), faded = true)
        Text(
            activity.description,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.medium)).background(com.project.solaria_mobile.core.designsystem.theme.containerFill()).padding(Spacing.md),
            color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyLarge,
        )
    }
    val context = LocalContext.current
    SectionHeading(stringResource(R.string.ps_opcoes), faded = true)
    // Fica sempre na lista (esmaecida quando a atividade não está aberta) para a sheet não mudar de altura ao trocar a situação.
    val editable = activity.status == ProjectStatusOption.OPEN
    ActionRow(stringResource(R.string.ps_editar_atividade), AppIcons.pencil, onEdit, Modifier.alpha(if (editable) 1f else 0.4f), compact = true, enabled = editable)
    if (start != null) ActionRow(stringResource(R.string.ps_adicionar_ao_calendario), AppIcons.calendar_days, {
        val insert = android.content.Intent(android.content.Intent.ACTION_INSERT)
            .setData(android.provider.CalendarContract.Events.CONTENT_URI)
            .putExtra(android.provider.CalendarContract.Events.TITLE, activity.title)
            .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, activity.description)
            .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.toInstant().toEpochMilli())
            .putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, (end ?: start.plusHours(1)).toInstant().toEpochMilli())
        try { context.startActivity(insert) } catch (_: android.content.ActivityNotFoundException) { }
    }, compact = true)
    val shareActivityTitle = stringResource(R.string.ps_compartilhar_atividade)
    ActionRow(stringResource(R.string.ps_compartilhar_atividade), AppIcons.share, {
        val text = listOfNotNull(
            activity.title,
            start?.let { "${it.format(DETAIL_DATE)}, ${end?.let { e -> "${it.format(TIME_LABEL)} – ${e.format(TIME_LABEL)}" } ?: it.format(TIME_LABEL)}" },
            activity.description.ifBlank { null },
        ).joinToString("\n")
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text)
        try { context.startActivity(android.content.Intent.createChooser(send, shareActivityTitle)) } catch (_: android.content.ActivityNotFoundException) { }
    }, compact = true)
}

private val DATE_LABEL = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("pt-BR"))
private val TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm")
