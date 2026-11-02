@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.home

import androidx.compose.foundation.layout.offset
import com.project.solaria_mobile.core.designsystem.components.ActionTile
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import kotlinx.coroutines.launch
import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.repeatOnLifecycle

import androidx.compose.ui.draw.clip
import com.project.solaria_mobile.feature.project.RecentProjectItem
import com.project.solaria_mobile.core.designsystem.components.ListItemDivider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.images.ImageLoaderViewModel
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.core.designsystem.theme.containerFill
import com.project.solaria_mobile.feature.communication.CallsBottomSheet
import com.project.solaria_mobile.presentation.navigation.AppDestination
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.Skeleton
import com.project.solaria_mobile.core.designsystem.components.SkeletonGroup
import com.project.solaria_mobile.core.designsystem.components.SkeletonListRow
import com.project.solaria_mobile.core.designsystem.components.SkeletonText
import androidx.compose.foundation.shape.CircleShape
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.feature.discovery.domain.ScheduleEvent
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeRoute(
    onOpenPage: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    images: ImageLoaderViewModel = hiltViewModel(),
    counter: com.project.solaria_mobile.feature.communication.NotificationCounterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) { viewModel.load() }
    val unread by counter.unread.collectAsStateWithLifecycle()
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.LaunchedEffect(owner, counter) {
        owner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            counter.refresh()
            kotlinx.coroutines.awaitCancellation()
        }
    }
    val bannerModel: HomeBannerViewModel = hiltViewModel()
    val banner by bannerModel.banner.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, modifier, loading = { HomeSkeleton(modifier, banner) }) { content ->
        HomeScreen(content, viewModel::onCallsQueryChange, onOpenPage, modifier, images::bytes, unread, banner, bannerModel::select, bannerModel::setCustom)
    }
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onCallsQueryChange: (String) -> Unit,
    onOpenPage: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    loadImage: (suspend (String) -> ByteArray?)? = null,
    unreadCount: Long? = null,
    banner: HomeBanner = HomeBanner.Default,
    onBannerSelect: (BannerPreset) -> Unit = {},
    onBannerImage: (ByteArray) -> Unit = {},
) {
    var callsOpen by remember { mutableStateOf(false) }
    var bannerOpen by remember { mutableStateOf(false) }
    var invalidImage by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val picked = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.project.solaria_mobile.core.images.readPickedImage(context, uri) }
            if (picked == null) { invalidImage = true; bannerOpen = true } else { invalidImage = false; onBannerImage(picked.first) }
        }
    }
    val contentColor = banner.contentColor()

    Box(modifier.fillMaxSize().appBackground()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // O fundo escolhido fica atrás do cabeçalho; o conteúdo sobe sobre ele numa folha branca de cantos arredondados, como no perfil.
            Box(Modifier.fillMaxWidth().height(Sizing.homeBannerHeight)) {
                HomeBannerBackground(banner, Modifier.fillMaxSize())
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.screenTop),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Avatar(initials = state.profile?.initials.orEmpty(), modifier = Modifier.clickable { onOpenPage(AppDestination.ProfileSelf) }, color = AppTheme.colors.inputFill, textColor = AppTheme.colors.onSurface, imageUrl = state.profile?.avatarUrl, loadImage = loadImage)
                        Column {
                            Text(stringResource(R.string.hm_ola), style = MaterialTheme.typography.bodyMedium, color = contentColor.copy(alpha = 0.8f))
                            Text(state.profile?.name?.substringBefore(" ").orEmpty().ifBlank { stringResource(R.string.settings_group_account) }, style = MaterialTheme.typography.titleSmall, color = contentColor)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        RoundIconButton(AppIcons.images, stringResource(R.string.hm_alterar_fundo), { bannerOpen = true }, background = AppTheme.colors.surface)
                        RoundIconButton(if ((unreadCount ?: 0) > 0) AppIcons.bell_dot else AppIcons.bell,
                            unreadCount?.takeIf { it > 0 }?.let { stringResource(R.string.hm_notificacoes_nao_lidas, it) } ?: stringResource(R.string.nt_notificacoes),
                            { onOpenPage(AppDestination.Notifications) }, background = AppTheme.colors.surface)
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().offset(y = -Spacing.xxl).clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)).background(AppTheme.colors.surface)
                    .padding(horizontal = Spacing.screen).padding(top = Spacing.xl, bottom = Spacing.screenBottom + com.project.solaria_mobile.core.designsystem.theme.LocalBottomBarInset.current),
                verticalArrangement = Arrangement.spacedBy(Spacing.section),
            ) {
            // Sem compromissos hoje e sem projetos, "Hoje" e "Projetos recentes" só repetiriam frases vazias: no lugar entra a grade de atalhos.
            val fresh = (state.today as? TodayAgenda.Ready)?.events?.isEmpty() == true && state.recentProjects.isEmpty()
            if (fresh) StartGrid(onOpenPage) else TodaySection(state.today, onOpenPage)
            if (state.pendingInvitations > 0) {
                ActionRow(
                    if (state.pendingInvitations == 1) stringResource(R.string.hm_1_convite_de_projeto) else "${state.pendingInvitations} convites de projeto",
                    AppIcons.mail,
                    { onOpenPage(AppDestination.Projects) },
                    subtitle = stringResource(R.string.hm_aguardando_sua_resposta),
                )
            }
            if (!fresh) Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeading(stringResource(R.string.hm_projetos_recentes), action = stringResource(R.string.hm_ver_todos), navigation = true, onAction = { onOpenPage(AppDestination.Projects) })
                if (state.recentProjects.isEmpty()) Text(stringResource(R.string.hm_seus_projetos_aparecerao_aqui), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                else Column(Modifier.clip(RoundedCornerShape(Radius.large)).background(containerFill())) {
                    state.recentProjects.take(3).forEachIndexed { index, project ->
                        if (index > 0) ListItemDivider(Modifier.padding(horizontal = Spacing.md))
                        RecentProjectItem(project, { onOpenPage(AppDestination.Project(project.id)) })
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeading(stringResource(R.string.hm_seu_espaco))
                ActionRow(stringResource(R.string.as_assistente), AppIcons.sparkles, { onOpenPage(AppDestination.Assistant) })
                // Os atalhos que a grade já mostra não se repetem na lista.
                if (!fresh) ActionRow(stringResource(R.string.pd_agenda), AppIcons.calendar_days, { onOpenPage(AppDestination.Schedule) })
                ActionRow(stringResource(R.string.hm_chamados), AppIcons.phone, { callsOpen = true })
                if (!fresh) ActionRow(stringResource(R.string.pf_conexoes), AppIcons.users, { onOpenPage(AppDestination.Connections) })
                state.profile?.companyId?.let { id -> ActionRow(stringResource(R.string.hm_sua_empresa), AppIcons.building, { onOpenPage(AppDestination.Enterprise(id)) }) }
                if (!fresh) ActionRow(stringResource(R.string.pd_novo_projeto), AppIcons.folder_plus, { onOpenPage(AppDestination.NewProject) })
                ActionRow(stringResource(R.string.settings_title), AppIcons.settings, { onOpenPage(AppDestination.Settings) })
            }
            }
        }
    }

    if (bannerOpen) {
        BannerSheet(
            current = banner,
            onSelect = onBannerSelect,
            onPickImage = { picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            invalidImage = invalidImage,
            onDismiss = { bannerOpen = false; invalidImage = false },
        )
    }

    if (callsOpen) {
        CallsBottomSheet(
            calls = state.recentCalls,
            query = state.callsQuery,
            failure = state.callsFailure,
            loading = state.callsLoading,
            onQueryChange = onCallsQueryChange,
            onDismiss = { callsOpen = false },
            onOpenCall = { id -> callsOpen = false; onOpenPage(AppDestination.Call(id)) },
            onOpenAll = { callsOpen = false; onOpenPage(AppDestination.Calls) },
        )
    }
}

/** Mesma geometria da Home: cabeçalho, compromissos de hoje, bloco de recentes e atalhos de "Seu espaço". */
@Composable
private fun HomeSkeleton(modifier: Modifier = Modifier, banner: HomeBanner = HomeBanner.Default) {
    Box(modifier.fillMaxSize().appBackground()) {
        // O fundo é local e já aparece; só o que vem do servidor fica em esqueleto.
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(Sizing.homeBannerHeight)) { HomeBannerBackground(banner, Modifier.fillMaxSize()) }
            Box(Modifier.fillMaxWidth().weight(1f).offset(y = -Spacing.xxl).clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)).background(AppTheme.colors.surface))
        }
        SkeletonGroup(
            Modifier.padding(horizontal = Spacing.screen).padding(top = Spacing.screenTop),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            Row(Modifier.fillMaxWidth().height(Sizing.homeBannerHeight - Spacing.screenTop - Spacing.xxl + Spacing.xl), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                SkeletonListRow(Modifier.fillMaxWidth(0.6f), lines = 2)
                Skeleton(Modifier.size(Sizing.headerButton), CircleShape)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Box(Modifier.height(Sizing.touchTarget), contentAlignment = Alignment.CenterStart) { SkeletonText(0.25f, height = Spacing.xl) }
                SkeletonListRow(Modifier.fillMaxWidth(), lines = 2)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Box(Modifier.height(Sizing.touchTarget), contentAlignment = Alignment.CenterStart) { SkeletonText(0.5f, height = Spacing.xl) }
                Column(Modifier.clip(RoundedCornerShape(Radius.large)).background(containerFill())) {
                    repeat(3) {
                        Row(Modifier.padding(vertical = Spacing.md, horizontal = Spacing.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Skeleton(Modifier.width(Sizing.thumbnailWidth).height(Sizing.thumbnailHeight))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                SkeletonText(0.6f, height = Spacing.lg)
                                SkeletonText(0.85f)
                                SkeletonText(0.45f)
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SkeletonText(0.35f, height = Spacing.xl)
                repeat(3) { Skeleton(Modifier.fillMaxWidth().height(Sizing.actionHeight)) }
            }
        }
    }
}

/** Grade de atalhos para quem ainda não tem compromisso nem projeto; os mesmos cartões cinza da tela do projeto. */
@Composable
private fun StartGrid(onOpenPage: (AppDestination) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SectionHeading(stringResource(R.string.hm_o_que_fazer))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ActionTile(stringResource(R.string.pd_novo_projeto), AppIcons.folder_plus, { onOpenPage(AppDestination.NewProject) }, Modifier.weight(1f).fillMaxHeight())
            ActionTile(stringResource(R.string.pd_agenda), AppIcons.calendar_days, { onOpenPage(AppDestination.Schedule) }, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ActionTile(stringResource(R.string.pf_conexoes), AppIcons.users, { onOpenPage(AppDestination.Connections) }, Modifier.weight(1f).fillMaxHeight())
            ActionTile(stringResource(R.string.pd_conversas), AppIcons.message_circle, { onOpenPage(AppDestination.Chats) }, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

/** Compromissos de hoje vindos das atividades dos projetos, com estado próprio quando a agenda não carrega. */
@Composable
private fun TodaySection(today: TodayAgenda, onOpenPage: (AppDestination) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeading(stringResource(R.string.hm_hoje), action = stringResource(R.string.hm_ver_agenda), navigation = true, onAction = { onOpenPage(AppDestination.Schedule) })
        when (today) {
            TodayAgenda.Loading -> SkeletonGroup { SkeletonListRow(Modifier.fillMaxWidth(), lines = 2) }
            TodayAgenda.Unavailable -> Text(stringResource(R.string.hm_nao_foi_possivel_carregar_a), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
            is TodayAgenda.Ready -> if (today.events.isEmpty()) {
                Text(stringResource(R.string.hm_nenhum_compromisso_para_hoje), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
            } else {
                val zone = ZoneId.systemDefault()
                val date = LocalDate.now(zone)
                Column(Modifier.clip(RoundedCornerShape(Radius.large)).background(containerFill())) {
                    today.events.forEachIndexed { index, event ->
                        if (index > 0) ListItemDivider(Modifier.padding(horizontal = Spacing.md))
                        TodayEventRow(event, todayTimeLabel(event, date, zone)) { onOpenPage(AppDestination.Project(event.projectId)) }
                    }
                    if (today.more > 0) {
                        ListItemDivider(Modifier.padding(horizontal = Spacing.md))
                        Text(
                            if (today.more == 1) stringResource(R.string.hm_mais_1_compromisso_na_agenda) else "Mais ${today.more} compromissos na agenda",
                            Modifier.fillMaxWidth().clickable { onOpenPage(AppDestination.Schedule) }.padding(Spacing.md),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.onSurfaceMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayEventRow(event: ScheduleEvent, time: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(time, Modifier.width(56.dp), style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface)
        Column(Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(event.projectName, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.onSurfaceMuted, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

/** Horário de início no fuso do aparelho; atividade que começou antes de hoje aparece como em andamento. */
internal fun todayTimeLabel(event: ScheduleEvent, today: LocalDate, zone: ZoneId): String {
    val start = event.start(zone)
    return if (start.toLocalDate() == today) start.format(DateTimeFormatter.ofPattern("HH:mm")) else "Em curso"
}
