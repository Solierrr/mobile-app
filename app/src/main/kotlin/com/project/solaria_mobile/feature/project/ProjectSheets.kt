@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.project

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
import androidx.compose.material3.TextButton
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
import com.project.solaria_mobile.core.designsystem.components.GallerySkeleton

enum class ProjectSheet {
    LOCATION,
    GALLERY,
    AGENDA,
    CONVERSATIONS,
    HISTORY,
    TEAM,
}

private val sectionModifier = Modifier.fillMaxWidth()

@Composable
internal fun ProjectSheetContent(
    sheet: ProjectSheet,
    viewModel: ProjectSectionsViewModel,
    onNavigate: (AppDestination) -> Unit,
    onCopy: (String) -> Unit,
    detail: String? = null,
    onOpenDetail: (String) -> Unit = {},
    onCloseDetail: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.sm, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (detail != null) {
            SheetDetail(detail, viewModel, onNavigate, onCloseDetail, onOpenDetail)
            return@Column
        }
        when (sheet) {
            ProjectSheet.LOCATION -> LocationSection(viewModel, onNavigate, onCopy)
            ProjectSheet.GALLERY -> GallerySection(viewModel) { onOpenDetail(PHOTO_DETAIL + it) }
            ProjectSheet.AGENDA -> AgendaSection(viewModel) { onOpenDetail(if (it == NEW_ACTIVITY) ACTIVITY_FORM + NEW_ACTIVITY else ACTIVITY_DETAIL + it) }
            ProjectSheet.CONVERSATIONS -> com.project.solaria_mobile.feature.communication.ProjectCommunitySection(onNavigate)
            ProjectSheet.HISTORY -> HistorySection(viewModel)
            ProjectSheet.TEAM -> TeamSection(viewModel, onNavigate, onOpenDetail)
        }
    }
}

@Composable
private fun LocationSection(viewModel: ProjectSectionsViewModel, onNavigate: (AppDestination) -> Unit, onCopy: (String) -> Unit) {
    val state by viewModel.location.collectAsStateWithLifecycle()
    UiStateHost(state, { viewModel.retry(ProjectSheet.LOCATION) }, sectionModifier, inline = true, loading = { ContentSkeleton() }) { info ->
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
                    style = if (index == 0) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                )
            }
            ActionRow("Abrir no mapa", AppIcons.map_pin, { onNavigate(AppDestination.Map(viewModel.projectId)) })
            ActionRow("Copiar endereço", AppIcons.copy, { onCopy(info.copyText) })
        }
    }
}

@Composable
private fun GallerySection(viewModel: ProjectSectionsViewModel, onOpenPhoto: (String) -> Unit) {
    val state by viewModel.photos.collectAsStateWithLifecycle()
    val action by viewModel.photoAction.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val picked = withContext(Dispatchers.IO) { readPickedImage(context, uri) }
            if (picked == null) viewModel.rejectPhoto() else viewModel.uploadPhoto(picked.first, picked.second)
        }
    }
    ActionRow("Adicionar imagem", AppIcons.images, {
        viewModel.clearPhotoAction()
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }, compact = true)
    ProjectActionStatus(action, working = "Enviando imagem…")
    UiStateHost(state, { viewModel.retry(ProjectSheet.GALLERY) }, sectionModifier, inline = true, emptyMessage = "Este projeto ainda não tem imagens.", loading = { GallerySkeleton() }) { photos ->
        Text(
            if (photos.size == 1) "1 imagem" else "${photos.size} imagens",
            color = AppTheme.colors.onSurfaceMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        photos.forEach { photo ->
            RemoteImage(
                key = photo.id,
                load = viewModel::photoContent,
                description = if (photo.cover) "Capa do projeto" else "Imagem do projeto",
                modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(Radius.large)).clickable { onOpenPhoto(photo.id) },
            )
        }
    }
}

@Composable
private fun AgendaSection(viewModel: ProjectSectionsViewModel, onOpenActivity: (String) -> Unit) {
    val state by viewModel.activities.collectAsStateWithLifecycle()
    ActionRow("Nova atividade", AppIcons.calendar_days, { viewModel.clearActivityAction(); onOpenActivity(NEW_ACTIVITY) }, compact = true)
    UiStateHost(state, { viewModel.retry(ProjectSheet.AGENDA) }, sectionModifier, inline = true, emptyMessage = "Nenhuma atividade agendada.", loading = { ListSkeleton(rows = 3, circle = false) }) { activities ->
        SectionHeading("Atividades do projeto", faded = true)
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
private fun HistorySection(viewModel: ProjectSectionsViewModel) {
    val state by viewModel.history.collectAsStateWithLifecycle()
    UiStateHost(state, { viewModel.retry(ProjectSheet.HISTORY) }, sectionModifier, inline = true, emptyMessage = "Nada no histórico ainda.", loading = { ListSkeleton(rows = 4, leading = Spacing.md) }) { entries ->
        SectionHeading("Atividade recente", faded = true)
        entries.forEach { TimelineEntry(it.title, it.detail) }
    }
}

@Composable
private fun TeamSection(viewModel: ProjectSectionsViewModel, onNavigate: (AppDestination) -> Unit, onOpenDetail: (String) -> Unit) {
    val state by viewModel.members.collectAsStateWithLifecycle()
    val invitations by viewModel.invitations.collectAsStateWithLifecycle()
    ActionRow("Convidar pessoa", AppIcons.user_round_plus, { viewModel.clearTeamAction(); onOpenDetail(INVITE_DETAIL) }, compact = true)
    UiStateHost(state, { viewModel.retry(ProjectSheet.TEAM) }, sectionModifier, inline = true, emptyMessage = "Nenhuma pessoa neste projeto.", loading = { ListSkeleton(rows = 4) }) { members ->
        SectionHeading("Pessoas envolvidas", faded = true, action = "Ver conexões", onAction = { onNavigate(AppDestination.Connections) })
        members.forEachIndexed { index, member -> TeamEntry(member.initials, member.name, member.role, index) { onOpenDetail(MEMBER_DETAIL + member.id) } }
        if (invitations.isNotEmpty()) {
            SectionHeading("Convites pendentes", faded = true)
            invitations.forEach { invitation ->
                ActionRow(invitation.email, AppIcons.mail, { viewModel.clearTeamAction(); onOpenDetail(INVITATION_DETAIL + invitation.id) },
                    subtitle = listOf(invitation.role, invitation.expires).filter { it.isNotBlank() }.joinToString(" · "), compact = true)
            }
        }
    }
}

@Composable
private fun TimelineEntry(title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.padding(top = 7.dp).size(9.dp).clip(CircleShape).background(AppTheme.colors.onSurface))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
            if (detail.isNotBlank()) Text(detail, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        }
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

private const val PHOTO_DETAIL = "photo:"
private const val MEMBER_DETAIL = "member:"
private const val ACTIVITY_DETAIL = "activity:"
private const val INVITE_DETAIL = "invite"
private const val NEW_ACTIVITY = "new"
private const val ACTIVITY_FORM = "activity-form:"
private const val INVITATION_DETAIL = "invitation:"

/** Segundo nível de uma seção: foto da galeria ou pessoa dos envolvidos, com retorno ao nível anterior. */
@Composable
private fun SheetDetail(detail: String, viewModel: ProjectSectionsViewModel, onNavigate: (AppDestination) -> Unit, onClose: () -> Unit, onOpenDetail: (String) -> Unit) {
    when {
        detail.startsWith(PHOTO_DETAIL) -> {
            val photos by viewModel.photos.collectAsStateWithLifecycle()
            val photo = (photos as? UiState.Content)?.data?.firstOrNull { it.id == detail.removePrefix(PHOTO_DETAIL) }
            val photoAction by viewModel.photoAction.collectAsStateWithLifecycle()
            // Depois de remover, a foto sai da galeria recarregada e o segundo nível volta à galeria.
            LaunchedEffect(photo, photoAction) { if (photo == null && photos is UiState.Content && photoAction == null) onClose() }
            SheetBackRow("Voltar para a galeria", onClose)
            if (photo == null) {
                StateNotice("Imagem indisponível", "Esta imagem não está mais no projeto.", icon = AppIcons.images)
            } else {
                RemoteImage(
                    key = photo.id,
                    load = viewModel::photoContent,
                    description = if (photo.cover) "Capa do projeto" else "Imagem do projeto",
                    modifier = Modifier.fillMaxWidth().height(PHOTO_DETAIL_HEIGHT).clip(RoundedCornerShape(Radius.large)).background(AppTheme.colors.inputFill),
                    contentScale = ContentScale.Fit,
                )
                Text(if (photo.cover) "Capa do projeto" else "Imagem do projeto", color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                PhotoActions(photo.id, photo.cover, viewModel)
            }
        }
        detail.startsWith(ACTIVITY_FORM) -> {
            val activities by viewModel.activities.collectAsStateWithLifecycle()
            val editing = detail.removePrefix(ACTIVITY_FORM).takeIf { it != NEW_ACTIVITY }
            val activity = editing?.let { id -> (activities as? UiState.Content)?.data?.firstOrNull { it.id == id } }
            SheetBackRow("Voltar para a agenda", onClose)
            if (editing != null && activity == null) {
                StateNotice("Atividade indisponível", "Esta atividade não está mais no projeto.", icon = AppIcons.calendar_days)
            } else {
                ActivityForm(activity, viewModel, onClose)
            }
        }
        detail == INVITE_DETAIL -> {
            SheetBackRow("Voltar para os envolvidos", onClose)
            InviteForm(viewModel, onClose)
        }
        detail.startsWith(INVITATION_DETAIL) -> {
            val invitations by viewModel.invitations.collectAsStateWithLifecycle()
            val invitation = invitations.firstOrNull { it.id == detail.removePrefix(INVITATION_DETAIL) }
            val action by viewModel.teamAction.collectAsStateWithLifecycle()
            LaunchedEffect(action) { if ((action as? ProjectAction.Done)?.operation == ProjectOperation.REVOKE_INVITE) onClose() }
            SheetBackRow("Voltar para os envolvidos", onClose)
            if (invitation == null) {
                StateNotice("Convite indisponível", "Este convite já foi respondido ou cancelado.", icon = AppIcons.mail)
            } else {
                Text(invitation.email, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
                Text(listOf(invitation.role, invitation.expires).filter { it.isNotBlank() }.joinToString(" · "),
                    color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                ActionRow("Reenviar convite", AppIcons.mail, { viewModel.clearTeamAction(); viewModel.resendInvitation(invitation.id) }, compact = true)
                ActionRow("Cancelar convite", AppIcons.users, { viewModel.clearTeamAction(); viewModel.revokeInvitation(invitation.id) }, compact = true)
                if ((action as? ProjectAction.Done)?.operation == ProjectOperation.RESEND_INVITE) {
                    Text("Convite reenviado.", color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                }
                ProjectActionStatus(action, working = "Salvando…")
            }
        }
        detail.startsWith(ACTIVITY_DETAIL) -> {
            val activities by viewModel.activities.collectAsStateWithLifecycle()
            val activity = (activities as? UiState.Content)?.data?.firstOrNull { it.id == detail.removePrefix(ACTIVITY_DETAIL) }
            val action by viewModel.activityAction.collectAsStateWithLifecycle()
            SheetBackRow("Voltar para a agenda", onClose)
            if (activity == null) {
                StateNotice("Atividade indisponível", "Esta atividade não está mais no projeto.", icon = AppIcons.calendar_days)
            } else {
                Text(activity.title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
                if (activity.subtitle.isNotBlank()) Text(listOf(activity.subtitle, activity.time).filter { it.isNotBlank() }.joinToString(" · "),
                    color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                activity.status?.let { StatusBadge(it.label, tint = projectStatusTint(it.label)) }
                Text("Situação da atividade", color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
                ProjectStatusOption.entries.forEach { option ->
                    val current = option == activity.status
                    ActionRow(option.label, if (current) AppIcons.check else AppIcons.clock,
                        { if (!current) { viewModel.clearActivityAction(); viewModel.changeActivityStatus(activity.id, option) } },
                        trailing = if (current) "Atual" else null, compact = true)
                }
                if (activity.status == ProjectStatusOption.OPEN) {
                    ActionRow("Editar atividade", AppIcons.pencil, { viewModel.clearActivityAction(); onOpenDetail(ACTIVITY_FORM + activity.id) }, compact = true)
                }
                ProjectActionStatus(action, working = "Salvando…")
            }
        }
        detail.startsWith(MEMBER_DETAIL) -> {
            val members by viewModel.members.collectAsStateWithLifecycle()
            val list = (members as? UiState.Content)?.data.orEmpty()
            val index = list.indexOfFirst { it.id == detail.removePrefix(MEMBER_DETAIL) }
            SheetBackRow("Voltar para os envolvidos", onClose)
            val member = list.getOrNull(index)
            if (member == null) {
                StateNotice("Pessoa indisponível", "Esta pessoa não participa mais do projeto.", icon = AppIcons.users)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Avatar(initials = member.initials, size = Sizing.avatarLarge, color = AvatarPalette(index))
                    Column {
                        Text(member.name, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
                        Text(member.role, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                ActionRow("Ver perfil", AppIcons.user_round_check, { onNavigate(AppDestination.ProfileOther(member.userId)) })
                if (!member.owner) RemoveMember(member.userId, viewModel, onClose)
            }
        }
    }
}

@Composable
private fun SheetBackRow(label: String, onClick: () -> Unit) {
    TextButton(onClick, Modifier.heightIn(min = Sizing.touchTarget)) {
        Icon(painterResource(AppIcons.chevron_left), null, Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurfaceMuted)
        Text(label, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelLarge)
    }
}

private val PHOTO_DETAIL_HEIGHT = 420.dp

@Composable
private fun PhotoActions(photoId: String, cover: Boolean, viewModel: ProjectSectionsViewModel) {
    val action by viewModel.photoAction.collectAsStateWithLifecycle()
    var confirmRemoval by remember(photoId) { mutableStateOf(false) }
    if (!cover) ActionRow("Definir como capa", AppIcons.check, { viewModel.clearPhotoAction(); viewModel.setCover(photoId) }, compact = true)
    if (confirmRemoval) {
        Text("Remover esta imagem do projeto?", color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
        PrimaryButton("Remover imagem", { viewModel.removePhoto(photoId) }, enabled = action != ProjectAction.Working)
        PrimaryButton("Cancelar", { confirmRemoval = false }, secondary = true)
    } else {
        ActionRow("Remover imagem", AppIcons.images, { viewModel.clearPhotoAction(); confirmRemoval = true }, compact = true)
    }
    ProjectActionStatus(action, working = "Salvando…")
}


@Composable
private fun RemoveMember(userId: String, viewModel: ProjectSectionsViewModel, onRemoved: () -> Unit) {
    val action by viewModel.teamAction.collectAsStateWithLifecycle()
    var confirm by remember(userId) { mutableStateOf(false) }
    LaunchedEffect(action) { if ((action as? ProjectAction.Done)?.operation == ProjectOperation.REMOVE_MEMBER) onRemoved() }
    if (confirm) {
        Text("Remover esta pessoa do projeto?", color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
        PrimaryButton("Remover do projeto", { viewModel.removeMember(userId) }, enabled = action != ProjectAction.Working)
        PrimaryButton("Cancelar", { confirm = false }, secondary = true)
    } else {
        ActionRow("Remover do projeto", AppIcons.users, { viewModel.clearTeamAction(); confirm = true }, compact = true)
    }
    ProjectActionStatus(action, working = "Salvando…")
}

/** Convite por e-mail com papel e função; o e-mail é conferido no aparelho antes de chamar o servidor. */
@Composable
private fun InviteForm(viewModel: ProjectSectionsViewModel, onSent: () -> Unit) {
    val action by viewModel.teamAction.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(ProjectRoleOption.MEMBER) }
    var functionName by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(action) { if ((action as? ProjectAction.Done)?.operation == ProjectOperation.INVITE) onSent() }
    Text("Convidar pessoa", color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
    FormField(label = "E-mail", value = email, onValueChange = { email = it }, keyboardType = KeyboardType.Email)
    Text("Papel no projeto", color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
    ProjectRoleOption.entries.forEach { option ->
        ActionRow(option.label, if (option == role) AppIcons.check else AppIcons.users, { role = option }, trailing = if (option == role) "Escolhido" else null, compact = true)
    }
    FormField(label = "Função (opcional)", value = functionName, onValueChange = { if (it.length <= MAX_FUNCTION_LENGTH) functionName = it })
    ProjectActionStatus(action, working = "Enviando convite…")
    PrimaryButton("Enviar convite", {
        if (EMAIL_PATTERN.matches(email.trim())) viewModel.invite(email, role, functionName) else viewModel.rejectInvite()
    }, enabled = action != ProjectAction.Working && email.isNotBlank())
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

    Text(if (activity == null) "Nova atividade" else "Editar atividade", color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
    FormField(label = "Título", value = draft.title, onValueChange = { update(draft.copy(title = it)) }, maxLength = 160,
        error = if (ActivityField.TITLE in errors) "Informe um título de até 160 caracteres." else null)
    FormField(label = "Descrição (opcional)", value = draft.description, onValueChange = { update(draft.copy(description = it)) },
        singleLine = false, minLines = 2, maxLength = 4000)
    Text("Tipo", color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
    if (activity == null) {
        ActivityTypeOption.entries.forEach { option ->
            ActionRow(option.label, if (option == draft.type) AppIcons.check else AppIcons.calendar_days, { update(draft.copy(type = option)) },
                trailing = if (option == draft.type) "Escolhido" else null, compact = true)
        }
    } else {
        Text(draft.type.label, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
    }
    ActionRow("Data", AppIcons.calendar_days, { picking = ActivityPicker.DATE }, trailing = draft.date.format(DATE_LABEL), compact = true)
    ActionRow("Início", AppIcons.clock, { picking = ActivityPicker.START }, trailing = draft.start.format(TIME_LABEL), compact = true)
    ActionRow("Término", AppIcons.clock, { picking = ActivityPicker.END }, trailing = draft.end.format(TIME_LABEL), compact = true)
    if (ActivityField.PERIOD in errors) Text("O término precisa ser depois do início.", color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
    ActionRow("No local do projeto", if (draft.onsite) AppIcons.check else AppIcons.map_pin, { update(draft.copy(onsite = !draft.onsite)) },
        trailing = if (draft.onsite) "Sim" else "Não", compact = true)
    val members = (membersState as? UiState.Content)?.data.orEmpty().filterNot { it.viewer }
    if (members.isNotEmpty()) {
        Text("Responsáveis", color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleSmall)
        members.forEach { member ->
            val chosen = member.userId in draft.assigneeIds
            ActionRow(member.name, if (chosen) AppIcons.check else AppIcons.users, {
                update(draft.copy(assigneeIds = if (chosen) draft.assigneeIds - member.userId else draft.assigneeIds + member.userId))
            }, subtitle = member.role, trailing = if (chosen) "Escolhido" else null, compact = true)
        }
    }
    ProjectActionStatus(action, working = "Salvando…")
    PrimaryButton(if (activity == null) "Criar atividade" else "Salvar atividade", { viewModel.saveActivity(activity?.id, draft) },
        enabled = action != ProjectAction.Working)

    when (picking) {
        ActivityPicker.DATE -> {
            val state = rememberDatePickerState(initialSelectedDateMillis = draft.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            DatePickerDialog(
                onDismissRequest = { picking = null },
                confirmButton = {
                    TextButton({
                        state.selectedDateMillis?.let { update(draft.copy(date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())) }
                        picking = null
                    }) { Text("Confirmar") }
                },
                dismissButton = { TextButton({ picking = null }) { Text("Cancelar") } },
            ) { DatePicker(state) }
        }
        ActivityPicker.START, ActivityPicker.END -> {
            val current = if (picking == ActivityPicker.START) draft.start else draft.end
            val state = rememberTimePickerState(current.hour, current.minute, is24Hour = true)
            AlertDialog(
                onDismissRequest = { picking = null },
                confirmButton = {
                    TextButton({
                        val chosen = LocalTime.of(state.hour, state.minute)
                        update(if (picking == ActivityPicker.START) draft.copy(start = chosen) else draft.copy(end = chosen))
                        picking = null
                    }) { Text("Confirmar") }
                },
                dismissButton = { TextButton({ picking = null }) { Text("Cancelar") } },
                text = { TimePicker(state) },
            )
        }
        null -> Unit
    }
}

private enum class ActivityPicker { DATE, START, END }

private val DATE_LABEL = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("pt-BR"))
private val TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm")
