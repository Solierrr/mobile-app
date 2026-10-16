package com.project.solaria_mobile.presentation.communication

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.MapBackdrop
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.SolariaAvatar
import com.project.solaria_mobile.core.designsystem.theme.SolariaGreen
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaInput
import com.project.solaria_mobile.core.designsystem.theme.SolariaLine
import com.project.solaria_mobile.core.designsystem.theme.SolariaOrange
import com.project.solaria_mobile.core.designsystem.theme.SolariaPink
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

private data class Conversation(
    val initials: String,
    val name: String,
    val preview: String,
    val time: String,
    val color: Color,
)

private val conversations = listOf(
    Conversation("RD", "Ricardo Dostoiévski", "O Will Smith, aquela lá do holiud", "17:40", SolariaOrange),
    Conversation("AM", "Alexandre de Moraes", "Tu vai apagar agora aquela mensagem?", "16:32", Color(0xFF45ADEB)),
    Conversation("BB", "Bruce Banner", "Cara, minha mão deu até hoje slk", "16:20", SolariaGreen),
    Conversation("AS", "Anthony Stark", "mas o thanos é um filho da mãe", "Há 20 dias", SolariaOrange),
    Conversation("FL", "Flash", "Tá certo isso? Eu não sou da DC??", "Mês passado", SolariaPink),
    Conversation("BN", "Bolonaro", "Prossegue com a compra e atraves...", "Mês passado", SolariaGreen),
    Conversation("FR", "Flash Reverso", "Tá errado isso! Eu sou da DC cara!", "Mês passado", SolariaOrange),
)

@Composable
fun ConversationsScreen(
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenProject: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredConversations = (conversations + conversations).filter {
        it.name.contains(query, ignoreCase = true) || it.preview.contains(query, ignoreCase = true)
    }
    Column(
        modifier = Modifier.fillMaxSize().background(SolariaWhite).padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg),
        verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
    ) {
        ScreenTitle("Conversas", onBack, trailingIcon = R.drawable.lucide_ic_at_sign, trailingLabel = "Iniciar conversa", onTrailingClick = onOpenChat)
        SectionHeading("Opções", faded = true)
        ActionRow("Iniciar conversa", R.drawable.lucide_ic_message_circle, onOpenChat, compact = true)
        ActionRow("Conversas de projetos", R.drawable.lucide_ic_folder, onOpenProject, compact = true)
        SectionHeading("Todas as conversas", faded = true)
        SearchField("Busque uma mensagem", query, { query = it }, trailingIcon = R.drawable.lucide_ic_sliders_horizontal, onTrailingClick = {})
        LazyColumn(verticalArrangement = Arrangement.spacedBy(SolariaSpace.xs)) {
            items(filteredConversations) { item ->
                ConversationRow(item = item, onClick = onOpenChat)
            }
        }
    }
}

@Composable
private fun ConversationRow(item: Conversation, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, color = SolariaWhite) {
        Row(
            modifier = Modifier.padding(vertical = SolariaSpace.xs),
            horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SolariaAvatar(item.initials, color = item.color)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.preview, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(item.time, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ChatScreen(onBack: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            "Olá! Você conseguiu revisar o cronograma do projeto?" to false,
            "Consegui sim. Vou enviar os detalhes ainda hoje." to true,
            "Perfeito, obrigado!" to false,
        )
    }
    Column(modifier = Modifier.fillMaxSize().background(SolariaWhite)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.md, bottom = SolariaSpace.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
        ) {
            RoundIconButton(R.drawable.lucide_ic_chevron_left, "Voltar", onBack, size = 42.dp)
            SolariaAvatar("RD", size = 42.dp, color = SolariaOrange, online = true)
            Column(modifier = Modifier.weight(1f)) {
                Text("Ricardo Dostoiévski", color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text("Disponível", color = SolariaGreen, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
            RoundIconButton(R.drawable.lucide_ic_phone, "Ligar", {}, size = 42.dp)
            RoundIconButton(R.drawable.lucide_ic_ellipsis, "Mais opções", {}, size = 42.dp)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SolariaLine))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = SolariaSpace.screen).padding(vertical = SolariaSpace.lg),
            verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
        ) {
            Text("Hoje", modifier = Modifier.align(Alignment.CenterHorizontally), color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            messages.forEach { (message, mine) ->
                MessageBubble(message = message, mine = mine)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.sm, bottom = SolariaSpace.md),
            horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(SolariaRadius.pill), color = SolariaInput, modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.padding(horizontal = SolariaSpace.lg, vertical = 14.dp),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = SolariaInk),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text("Escreva uma mensagem", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                        inner()
                    },
                )
            }
            Surface(
                modifier = Modifier.size(48.dp),
                onClick = { if (draft.isNotBlank()) { messages.add(draft to true); draft = "" } },
                shape = CircleShape,
                color = SolariaGreen,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.lucide_ic_send), contentDescription = "Enviar mensagem", tint = SolariaWhite, modifier = Modifier.size(SolariaSpace.icon))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: String, mine: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Surface(
            color = if (mine) Color(0xFFE9F7EF) else SolariaInput,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp),
        ) {
            Text(message, modifier = Modifier.padding(horizontal = SolariaSpace.md, vertical = SolariaSpace.sm), color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        Text("10:42", modifier = Modifier.padding(top = 3.dp), color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
    }
}

private data class ServiceCall(val title: String, val detail: String, val icon: Int, val badge: String? = null)

private val serviceCalls = listOf(
    ServiceCall("Chamado atribuído a você pelo seu supervisor", "Alocaram você e sua equipe como responsáveis pelo atendimento do chamado #1048.", R.drawable.lucide_ic_hash, "!!"),
    ServiceCall("Novo chamado para você!", "A empresa SolarTech abriu um chamado para manutenção em um projeto.", R.drawable.lucide_ic_phone, "Novo"),
    ServiceCall("Sua segurança está frágil", "Verifique seu e-mail o mais rápido possível. Clique aqui para verificar.", R.drawable.lucide_ic_shield_alert),
    ServiceCall("Novo chamado para você!", "A empresa SolarTech abriu um chamado para manutenção em um projeto.", R.drawable.lucide_ic_phone),
)

@Composable
fun CallsScreen(
    onBack: () -> Unit,
    onOpenCall: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredCalls = (serviceCalls + serviceCalls).filter {
        it.title.contains(query, ignoreCase = true) || it.detail.contains(query, ignoreCase = true)
    }
    Column(
        modifier = Modifier.fillMaxSize().background(SolariaWhite).padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg),
        verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
    ) {
        ScreenTitle("Chamados", onBack, trailingIcon = R.drawable.lucide_ic_sliders_horizontal, trailingLabel = "Filtrar", onTrailingClick = {})
        SearchField("Buscar chamado", query, { query = it }, trailingIcon = R.drawable.lucide_ic_sliders_horizontal, onTrailingClick = {})
        LazyColumn(verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
            item { SectionHeading("Recentes", faded = true) }
            items(filteredCalls) { call ->
                CallCard(call, onClick = onOpenCall)
            }
        }
    }
}

@Composable
fun CallsBottomSheet(
    onDismiss: () -> Unit,
    onOpenCall: () -> Unit,
    onOpenAll: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredCalls = serviceCalls.filter {
        it.title.contains(query, ignoreCase = true) || it.detail.contains(query, ignoreCase = true)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SolariaWhite,
        shape = RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.xxl, bottom = SolariaSpace.screenBottom),
            verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
        ) {
            ScreenTitle("Chamados", onBack = onDismiss, trailingIcon = R.drawable.lucide_ic_sliders_horizontal, trailingLabel = "Filtrar", onTrailingClick = {})
            SearchField("Buscar chamado", query, { query = it }, trailingIcon = R.drawable.lucide_ic_sliders_horizontal, onTrailingClick = {})
            SectionHeading("Recentes", faded = true, action = "Ver todos", onAction = onOpenAll)
            filteredCalls.forEach { call ->
                CallCard(call, onClick = onOpenCall)
            }
            Spacer(Modifier.height(SolariaSpace.lg))
        }
    }
}

@Composable
private fun CallCard(call: ServiceCall, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(SolariaRadius.medium),
        color = SolariaInput,
    ) {
        Row(
            modifier = Modifier.padding(SolariaSpace.md),
            horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painterResource(call.icon), contentDescription = null, tint = SolariaInk, modifier = Modifier.size(SolariaSpace.icon))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(call.title, color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                Text(call.detail, color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (call.badge != null) {
                Text(call.badge, color = if (call.badge == "!!") SolariaPink else SolariaGreen, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun CallDetailsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(SolariaWhite).verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(340.dp).background(Color(0xFFE8E8E8))) {
            MapBackdrop(modifier = Modifier.fillMaxSize())
            RoundIconButton(
                icon = R.drawable.lucide_ic_chevron_left,
                contentDescription = "Voltar",
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(start = SolariaSpace.screen, top = SolariaSpace.lg),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg, bottom = SolariaSpace.screenBottom),
            verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
        ) {
            Text("Há 22 km da localização atual", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Text("Google América Latina", color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                ActionRow("Google Maps", R.drawable.lucide_ic_map_pin, {}, compact = true, modifier = Modifier.weight(1f))
                ActionRow("Copiar", R.drawable.lucide_ic_copy, {}, compact = true, modifier = Modifier.weight(1f))
            }
            SectionHeading("Opções", faded = true)
            ActionRow("Marcar como resolvido", R.drawable.lucide_ic_check, {})
            ActionRow("Entrar em contato", R.drawable.lucide_ic_message_circle, {})
            ActionRow("Denunciar", R.drawable.lucide_ic_shield_alert, {})
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(SolariaWhite).padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg)) {
        ScreenTitle("Notificações", onBack, trailingIcon = R.drawable.lucide_ic_bell_dot, trailingLabel = "Marcar tudo como lido", onTrailingClick = {})
        LazyColumn(modifier = Modifier.padding(top = SolariaSpace.md), verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
            item { SectionHeading("Recentes", faded = true) }
            items(serviceCalls + serviceCalls) { call ->
                CallCard(call, onClick = {})
            }
            item {
                Text("Você está em dia com suas notificações.", modifier = Modifier.fillMaxWidth().padding(SolariaSpace.xl), color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
