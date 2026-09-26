package com.project.solaria_mobile.presentation.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.SolariaAvatar
import com.project.solaria_mobile.core.designsystem.theme.SolariaCanvas
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaLine
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite
import kotlinx.coroutines.launch

private data class ProjectSummary(
    val name: String,
    val place: String,
    val status: String,
    val people: String,
)

private val projectSummaries = listOf(
    ProjectSummary("Fortech Enterprise", "São Paulo, SP", "Em andamento", "12 envolvidos"),
    ProjectSummary("Solar Tech", "Fortaleza, CE", "Em análise", "8 envolvidos"),
    ProjectSummary("Residencial Aurora", "Sorocaba, SP", "Em andamento", "6 envolvidos"),
    ProjectSummary("Unidade Paraíba", "Santana de Parnaíba, SP", "Concluído", "4 envolvidos"),
)

private enum class ProjectSheet(val title: String) {
    LOCATION("Informações do local"),
    GALLERY("Imagens do local"),
    AGENDA("Agenda do projeto"),
    CONVERSATIONS("Conversas do projeto"),
    HISTORY("Histórico"),
    TEAM("Envolvidos"),
}

@Composable
fun ProjectListScreen(
    onBack: () -> Unit,
    onOpenProject: () -> Unit,
    onOpenMap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SolariaWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SolariaSpace.screen)
            .padding(top = SolariaSpace.lg, bottom = SolariaSpace.screenBottom),
        verticalArrangement = Arrangement.spacedBy(SolariaSpace.lg),
    ) {
        ScreenTitle("Projetos", onBack, trailingIcon = R.drawable.lucide_ic_map_pin, trailingLabel = "Mapa", onTrailingClick = onOpenMap)
        SectionHeading("Seus projetos", faded = true)
        projectSummaries.forEachIndexed { index, project ->
            ProjectListCard(project = project, index = index, onClick = onOpenProject)
        }
        ActionRow("Criar projeto", R.drawable.lucide_ic_folder_plus, onClick = {})
    }
}

@Composable
private fun ProjectListCard(project: ProjectSummary, index: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(SolariaRadius.large),
        color = SolariaWhite,
        shadowElevation = 3.dp,
    ) {
        Column {
            Image(
                painter = painterResource(R.drawable.project_building),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(if (index == 0) 168.dp else 120.dp),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(SolariaSpace.md),
                verticalArrangement = Arrangement.spacedBy(SolariaSpace.xs),
            ) {
                Text(project.place, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                Text(project.name, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text(project.status, color = com.project.solaria_mobile.core.designsystem.theme.SolariaGreen, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    Text("·", color = SolariaInkMuted)
                    Text(project.people, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ProjectDetailScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    var activeSheet by remember { mutableStateOf<ProjectSheet?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SolariaWhite)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(345.dp)) {
            Image(
                painter = painterResource(R.drawable.project_building),
                contentDescription = "Fortech Enterprise",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SolariaSpace.screen)
                    .padding(top = SolariaSpace.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(R.drawable.lucide_ic_chevron_left, "Voltar", onBack)
                RoundIconButton(R.drawable.lucide_ic_ellipsis, "Mais opções", { menuOpen = true })
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -SolariaSpace.xxl)
                .clip(RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet))
                .background(SolariaWhite)
                .padding(horizontal = SolariaSpace.screen)
                .padding(top = SolariaSpace.xxl, bottom = SolariaSpace.screenBottom),
            verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
        ) {
            Text("São Paulo", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Text("Fortech Enterprise", color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(SolariaSpace.xs))
            ActionRow("Informações do local", R.drawable.lucide_ic_map_pin, { activeSheet = ProjectSheet.LOCATION })
            ActionRow("Imagens do local", R.drawable.lucide_ic_images, { activeSheet = ProjectSheet.GALLERY })
            ActionRow("Agenda do projeto", R.drawable.lucide_ic_calendar_days, { activeSheet = ProjectSheet.AGENDA }, badge = "10")
            ActionRow("Histórico", R.drawable.lucide_ic_clock, { activeSheet = ProjectSheet.HISTORY })
            ActionRow("Envolvidos", R.drawable.lucide_ic_building, { activeSheet = ProjectSheet.TEAM })
            ActionRow("Conversas", R.drawable.lucide_ic_message_circle, { activeSheet = ProjectSheet.CONVERSATIONS }, badge = "10")
            ActionRow("Empresa responsável", R.drawable.lucide_ic_building, { onNavigate("enterprise") })
            ActionRow("Configurações", R.drawable.lucide_ic_settings, { onNavigate("settings") })
        }
    }

    val sheet = activeSheet
    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = SolariaWhite,
            shape = RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet),
            dragHandle = null,
        ) {
            ProjectSheetContent(
                sheet = sheet,
                onBack = { scope.launch { sheetState.hide(); activeSheet = null } },
                onNavigate = { route -> scope.launch { sheetState.hide(); activeSheet = null; onNavigate(route) } },
            )
        }
    }

    if (menuOpen) {
        ModalBottomSheet(
            onDismissRequest = { menuOpen = false },
            sheetState = sheetState,
            containerColor = SolariaWhite,
            shape = RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet),
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.xxl, bottom = SolariaSpace.screenBottom),
                verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
            ) {
                ScreenTitle("Opções", onBack = { menuOpen = false })
                ActionRow("Listar projetos", R.drawable.lucide_ic_folder, { scope.launch { sheetState.hide(); menuOpen = false; onNavigate("projects") } }, trailing = "24 projetos")
                ActionRow("Novo projeto", R.drawable.lucide_ic_folder_plus, { menuOpen = false })
                ActionRow("Configurações", R.drawable.lucide_ic_settings, { scope.launch { sheetState.hide(); menuOpen = false; onNavigate("settings") } })
            }
        }
    }
}

@Composable
private fun ProjectSheetContent(
    sheet: ProjectSheet,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SolariaSpace.screen)
            .padding(top = SolariaSpace.xxl, bottom = SolariaSpace.screenBottom),
        verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
    ) {
        ScreenTitle(sheet.title, onBack)
        when (sheet) {
            ProjectSheet.LOCATION -> {
                Spacer(Modifier.height(SolariaSpace.xs))
                Text("Local do projeto", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                Text("Fortech Enterprise", color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Text("Av. das Nações Unidas, 12901 · São Paulo, SP", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                ActionRow("Abrir no mapa", R.drawable.lucide_ic_map_pin, { onNavigate("map") })
                ActionRow("Copiar endereço", R.drawable.lucide_ic_copy, {})
            }
            ProjectSheet.GALLERY -> {
                Image(
                    painter = painterResource(R.drawable.project_building),
                    contentDescription = "Imagem do projeto",
                    modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(SolariaRadius.large)),
                    contentScale = ContentScale.Crop,
                )
                Text("Local do projeto · 1 de 8 imagens", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                ActionRow("Adicionar imagem", R.drawable.lucide_ic_image_plus, {})
            }
            ProjectSheet.AGENDA -> {
                SectionHeading("Janeiro, 2026", faded = true)
                ActionRow("Visita técnica", R.drawable.lucide_ic_map_pin, { onNavigate("map") }, trailing = "09:30", subtitle = "4 jan · Fortech Enterprise")
                ActionRow("Alinhamento do projeto", R.drawable.lucide_ic_message_circle, { onNavigate("chat") }, trailing = "13:00", subtitle = "4 jan · Conversa com equipe")
                ActionRow("Revisar proposta", R.drawable.lucide_ic_file_text, {}, trailing = "16:15", subtitle = "4 jan · Solar Tech")
                ActionRow("Abrir agenda completa", R.drawable.lucide_ic_calendar_days, { onNavigate("schedule") })
            }
            ProjectSheet.CONVERSATIONS -> {
                SectionHeading("Conversas recentes", faded = true)
                TeamEntry("RC", "Ricardo Dostoiévski", "O Will Smith, aquela lá do holiud", 0, { onNavigate("chat") })
                TeamEntry("AM", "Alexandre de Moraes", "Tu vai apagar agora aquela mensagem?", 1, { onNavigate("chat") })
                TeamEntry("BB", "Bruce Banner", "Cara, minha mão deu até hoje slk", 2, { onNavigate("chat") })
                ActionRow("Ver todas as conversas", R.drawable.lucide_ic_messages_square, { onNavigate("chats") })
            }
            ProjectSheet.HISTORY -> {
                SectionHeading("Atividade recente", faded = true)
                TimelineEntry("Projeto atualizado", "Ronaldo Chagas · Hoje, 10:42")
                TimelineEntry("Novo chamado atribuído", "SolarTech · Ontem, 16:20")
                TimelineEntry("Endereço confirmado", "Ronaldo Chagas · 12 jun, 09:15")
                TimelineEntry("Projeto criado", "Você · 04 jun, 14:08")
            }
            ProjectSheet.TEAM -> {
                SectionHeading("Pessoas envolvidas", faded = true, action = "Ver conexões", onAction = { onNavigate("connections") })
                TeamEntry("RC", "Ronaldo Chagas", "Responsável pelo projeto", 0, { onNavigate("profile/other") })
                TeamEntry("ML", "Mariana Lima", "Engenheira elétrica", 1, { onNavigate("profile/other") })
                TeamEntry("FS", "Felipe Santos", "Instalação", 2, { onNavigate("profile/other") })
                TeamEntry("GC", "Gabriela Costa", "Fornecedor", 3, { onNavigate("profile/other") })
                ActionRow("Convidar pessoa", R.drawable.lucide_ic_user_plus, {})
            }
        }
    }
}

@Composable
private fun TimelineEntry(title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = SolariaSpace.xs),
        horizontalArrangement = Arrangement.spacedBy(SolariaSpace.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.padding(top = 7.dp).size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(com.project.solaria_mobile.core.designsystem.theme.SolariaOrange))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(detail, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TeamEntry(initials: String, name: String, role: String, index: Int, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = SolariaSpace.xs),
        horizontalArrangement = Arrangement.spacedBy(SolariaSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SolariaAvatar(initials = initials, color = com.project.solaria_mobile.core.designsystem.components.AvatarPalette(index))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(role, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        Icon(painter = painterResource(R.drawable.lucide_ic_ellipsis), contentDescription = "Mais opções de $name", tint = SolariaInkMuted, modifier = Modifier.size(SolariaSpace.icon))
    }
}
