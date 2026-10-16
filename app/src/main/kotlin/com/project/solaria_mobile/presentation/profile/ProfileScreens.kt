package com.project.solaria_mobile.presentation.profile

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
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
import com.project.solaria_mobile.core.designsystem.components.AvatarPalette
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.SolariaAvatar
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaLine
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite
import kotlinx.coroutines.launch

private data class UserProfile(
    val initials: String,
    val name: String,
    val handle: String,
    val state: String,
    val occupation: String,
    val phone: String,
)

private val ownProfile = UserProfile("RC", "Ronaldo Chagas", "@ronaldoa02349993", "São Paulo", "013110–200", "55+ (11) 91022–3438")
private val otherProfile = UserProfile("RN", "Rodrigo Nolabas", "@rodrigaoobatechagaas", "São Paulo", "Engenheiro Elétrico", "55+ (11) 91022–3438")

@Composable
fun ProfileScreen(
    self: Boolean,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val profile = if (self) ownProfile else otherProfile
    var infoOpen by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val banner = if (self) R.drawable.profile_waterfall else R.drawable.profile_night_sky

    Column(modifier = Modifier.fillMaxSize().background(SolariaWhite).verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            Image(painter = painterResource(banner), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(R.drawable.lucide_ic_chevron_left, "Voltar", onBack)
                RoundIconButton(
                    if (self) R.drawable.lucide_ic_pencil else R.drawable.lucide_ic_share,
                    if (self) "Editar perfil" else "Compartilhar perfil",
                    onClick = { if (self) infoOpen = true },
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().offset(y = -SolariaSpace.xxl).clip(RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet)).background(SolariaWhite).padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.xl, bottom = SolariaSpace.screenBottom),
            verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm), verticalAlignment = Alignment.CenterVertically) {
                SolariaAvatar(profile.initials, size = 52.dp, color = AvatarPalette(if (self) 0 else 1), online = !self)
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(profile.handle, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(SolariaSpace.sm))
            ProfileField("Estado", profile.state)
            ProfileField("Profissão", profile.occupation)
            ProfileField("Telefone", profile.phone)
            SectionHeading("Opções", faded = true)
            if (self) {
                ActionRow("Editar informações", R.drawable.lucide_ic_info, { infoOpen = true })
                ActionRow("Privacidade", R.drawable.lucide_ic_shield, { onNavigate("settings") })
                ActionRow("Compartilhar", R.drawable.lucide_ic_share, {})
                ActionRow("Configurações", R.drawable.lucide_ic_settings, { onNavigate("settings") })
            } else {
                ActionRow("Informações", R.drawable.lucide_ic_info, { infoOpen = true })
                ActionRow("Ver agenda", R.drawable.lucide_ic_calendar_days, { onNavigate("schedule") })
                ActionRow(if (connected) "Conectado" else "Adicionar conexão", R.drawable.lucide_ic_link, { connected = !connected })
                ActionRow("Conversar", R.drawable.lucide_ic_message_circle, { onNavigate("chat") })
                ActionRow("Compartilhar", R.drawable.lucide_ic_share, {})
            }
        }
    }

    if (infoOpen) {
        ModalBottomSheet(
            onDismissRequest = { infoOpen = false },
            sheetState = sheetState,
            containerColor = SolariaWhite,
            shape = RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet),
            dragHandle = null,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.xxl, bottom = SolariaSpace.screenBottom), verticalArrangement = Arrangement.spacedBy(SolariaSpace.md)) {
                ScreenTitle("Informações", onBack = { scope.launch { sheetState.hide(); infoOpen = false } })
                Text(profile.name, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Text("Perfil público", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                ProfileField("Estado", profile.state)
                ProfileField("Profissão", profile.occupation)
                ProfileField("Telefone", profile.phone)
                ActionRow("Compartilhar perfil", R.drawable.lucide_ic_share, {})
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = SolariaSpace.xs), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        Text(value, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SolariaLine))
}

private data class Connection(val initials: String, val name: String, val handle: String)

private val connections = listOf(
    Connection("EV", "Everson", "@eozoiogarai"),
    Connection("BB", "Bruce Banner", "@hulksmash"),
    Connection("SU", "Sasuke Uchiha", "@konohabush"),
    Connection("AN", "Ana Silva", "@anasilva"),
    Connection("ML", "Mariana Lima", "@marianalima"),
    Connection("FS", "Felipe Santos", "@felipesantos"),
)

@Composable
fun ConnectionsScreen(
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    var filterOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filteredConnections = (connections + connections).filter {
        it.name.contains(query, ignoreCase = true) || it.handle.contains(query, ignoreCase = true)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = Modifier.fillMaxSize().background(SolariaWhite).padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg)) {
        ScreenTitle("Conexões", onBack, trailingIcon = R.drawable.lucide_ic_ellipsis, trailingLabel = "Opções", onTrailingClick = { filterOpen = true })
        Row(modifier = Modifier.padding(top = SolariaSpace.md), horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
            SearchField("Busque um usuário", query, { query = it }, modifier = Modifier.weight(1f), trailingIcon = R.drawable.lucide_ic_sliders_horizontal, onTrailingClick = { filterOpen = true })
        }
        LazyColumn(modifier = Modifier.padding(top = SolariaSpace.md), verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
            items(filteredConnections) { user ->
                ConnectionRow(user, onClick = onOpenProfile)
            }
        }
    }

    if (filterOpen) {
        ModalBottomSheet(
            onDismissRequest = { filterOpen = false }, sheetState = sheetState, containerColor = SolariaWhite,
            shape = RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet), dragHandle = null,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.xxl, bottom = SolariaSpace.screenBottom), verticalArrangement = Arrangement.spacedBy(SolariaSpace.md)) {
                ScreenTitle("Conexões", onBack = { filterOpen = false })
                ActionRow("Aguardando aprovação", R.drawable.lucide_ic_user_round_check, { filterOpen = false })
                ActionRow("Convidar pessoa", R.drawable.lucide_ic_user_round_plus, { filterOpen = false })
                ActionRow("Conexões por projeto", R.drawable.lucide_ic_folder, { filterOpen = false })
            }
        }
    }
}

@Composable
private fun ConnectionRow(connection: Connection, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, color = SolariaWhite) {
        Row(modifier = Modifier.padding(vertical = SolariaSpace.xs), horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm), verticalAlignment = Alignment.CenterVertically) {
            SolariaAvatar(connection.initials, color = AvatarPalette(connection.name.length))
            Column(modifier = Modifier.weight(1f)) {
                Text(connection.name, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(connection.handle, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
            Icon(painter = painterResource(R.drawable.lucide_ic_ellipsis), contentDescription = "Opções de ${connection.name}", tint = SolariaInkMuted, modifier = Modifier.size(SolariaSpace.icon))
        }
    }
}

@Composable
fun EnterpriseScreen(onBack: () -> Unit, onOpenProfile: () -> Unit, onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(SolariaWhite).verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().height(255.dp)) {
            Image(painter = painterResource(R.drawable.project_building), contentDescription = "Fortech Enterprise", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            RoundIconButton(R.drawable.lucide_ic_chevron_left, "Voltar", onBack, modifier = Modifier.padding(start = SolariaSpace.screen, top = SolariaSpace.lg))
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg, bottom = SolariaSpace.screenBottom), verticalArrangement = Arrangement.spacedBy(SolariaSpace.md)) {
            SectionHeading("Empresa", faded = true)
            Text("Fortech Enterprise", color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Text("Energia limpa, projetos que aproximam pessoas e negócios.", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            ProfileField("Localização", "São Paulo, SP")
            ProfileField("Projetos ativos", "24")
            ProfileField("Na plataforma desde", "2024")
            SectionHeading("Pessoas", faded = true, action = "Ver todas", onAction = { onNavigate("connections") })
            Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                repeat(4) { index -> SolariaAvatar(listOf("FC", "ML", "FS", "GC")[index], color = AvatarPalette(index), modifier = Modifier.clickable(onClick = onOpenProfile)) }
            }
            ActionRow("Ver projetos", R.drawable.lucide_ic_folder, { onNavigate("projects") })
            ActionRow("Conversar", R.drawable.lucide_ic_message_circle, { onNavigate("chats") })
            ActionRow("Localização", R.drawable.lucide_ic_map_pin, { onNavigate("map") })
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenProfile: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val settingsGroups = listOf(
        "Personalização" to listOf(
            SettingAction("Alterar informações", R.drawable.lucide_ic_info, onOpenProfile),
            SettingAction("E-mail", R.drawable.lucide_ic_mail),
        ),
        "Privacidade" to listOf(
            SettingAction("Marcar como resolvido", R.drawable.lucide_ic_check),
            SettingAction("Entrar em contato", R.drawable.lucide_ic_message_circle),
            SettingAction("Denunciar", R.drawable.lucide_ic_shield_alert),
        ),
        "Integrações" to listOf(
            SettingAction("Google Maps", R.drawable.lucide_ic_map_pin),
            SettingAction("Google Agenda", R.drawable.lucide_ic_calendar_days),
        ),
        "Preferências" to listOf(
            SettingAction("Notificações", R.drawable.lucide_ic_bell),
            SettingAction("Privacidade da conta", R.drawable.lucide_ic_lock),
            SettingAction("Sair da conta", R.drawable.lucide_ic_log_out),
        ),
    )
    Column(modifier = Modifier.fillMaxSize().background(SolariaWhite).padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(SolariaSpace.md)) {
        ScreenTitle("Configurações", onBack, trailingIcon = R.drawable.lucide_ic_circle_question_mark, trailingLabel = "Ajuda", onTrailingClick = {})
        SearchField("Procure o que quiser", query, { query = it }, trailingIcon = R.drawable.lucide_ic_sliders_horizontal, onTrailingClick = {})
        settingsGroups.forEach { (title, actions) ->
            val filteredActions = actions.filter { it.title.contains(query, ignoreCase = true) }
            if (filteredActions.isNotEmpty()) {
                SectionHeading(title, faded = true)
                filteredActions.forEach { action -> ActionRow(action.title, action.icon, action.onClick) }
            }
        }
        Spacer(Modifier.height(SolariaSpace.xxl))
    }
}

private data class SettingAction(
    val title: String,
    val icon: Int,
    val onClick: () -> Unit = {},
)
