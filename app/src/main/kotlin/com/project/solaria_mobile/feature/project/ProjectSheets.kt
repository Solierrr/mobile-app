@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.Avatar
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.presentation.navigation.AppDestination

internal enum class ProjectSheet(val title: String) {
    LOCATION("Informações do local"),
    GALLERY("Imagens do local"),
    AGENDA("Agenda do projeto"),
    CONVERSATIONS("Conversas do projeto"),
    HISTORY("Histórico"),
    TEAM("Envolvidos"),
}

@Composable
internal fun ProjectSheetContent(
    sheet: ProjectSheet,
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
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
            ProjectSheet.LOCATION -> {
                Spacer(Modifier.height(Spacing.xs))
                Text("Local do projeto", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                Text("Fortech Enterprise", color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Text("Av. das Nações Unidas, 12901 · São Paulo, SP", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                ActionRow("Abrir no mapa", LucideR.drawable.lucide_ic_map_pin, { onNavigate(AppDestination.Map) })
                ActionRow("Copiar endereço", LucideR.drawable.lucide_ic_copy, {})
            }
            ProjectSheet.GALLERY -> {
                Image(
                    painter = painterResource(R.drawable.project_building),
                    contentDescription = "Imagem do projeto",
                    modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(Radius.large)),
                    contentScale = ContentScale.Crop,
                )
                Text("Local do projeto · 1 de 8 imagens", color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                ActionRow("Adicionar imagem", LucideR.drawable.lucide_ic_image_plus, {})
            }
            ProjectSheet.AGENDA -> {
                SectionHeading("Janeiro, 2026", faded = true)
                ActionRow("Visita técnica", LucideR.drawable.lucide_ic_map_pin, { onNavigate(AppDestination.Map) }, trailing = "09:30", subtitle = "4 jan · Fortech Enterprise")
                ActionRow("Alinhamento do projeto", LucideR.drawable.lucide_ic_message_circle, { onNavigate(AppDestination.Chat("c1")) }, trailing = "13:00", subtitle = "4 jan · Conversa com equipe")
                ActionRow("Revisar proposta", LucideR.drawable.lucide_ic_file_text, {}, trailing = "16:15", subtitle = "4 jan · Solar Tech")
                ActionRow("Abrir agenda completa", LucideR.drawable.lucide_ic_calendar_days, { onNavigate(AppDestination.Schedule) })
            }
            ProjectSheet.CONVERSATIONS -> {
                SectionHeading("Conversas recentes", faded = true)
                TeamEntry("RC", "Ricardo Dostoiévski", "O Will Smith, aquela lá do holiud", 0, { onNavigate(AppDestination.Chat("c1")) })
                TeamEntry("AM", "Alexandre de Moraes", "Tu vai apagar agora aquela mensagem?", 1, { onNavigate(AppDestination.Chat("c2")) })
                TeamEntry("BB", "Bruce Banner", "Cara, minha mão deu até hoje slk", 2, { onNavigate(AppDestination.Chat("c3")) })
                ActionRow("Ver todas as conversas", LucideR.drawable.lucide_ic_messages_square, { onNavigate(AppDestination.Chats) })
            }
            ProjectSheet.HISTORY -> {
                SectionHeading("Atividade recente", faded = true)
                TimelineEntry("Projeto atualizado", "Ronaldo Chagas · Hoje, 10:42")
                TimelineEntry("Novo chamado atribuído", "SolarTech · Ontem, 16:20")
                TimelineEntry("Endereço confirmado", "Ronaldo Chagas · 12 jun, 09:15")
                TimelineEntry("Projeto criado", "Você · 04 jun, 14:08")
            }
            ProjectSheet.TEAM -> {
                SectionHeading("Pessoas envolvidas", faded = true, action = "Ver conexões", onAction = { onNavigate(AppDestination.Connections) })
                TeamEntry("RC", "Ronaldo Chagas", "Responsável pelo projeto", 0, { onNavigate(AppDestination.ProfileOther("u1")) })
                TeamEntry("ML", "Mariana Lima", "Engenheira elétrica", 1, { onNavigate(AppDestination.ProfileOther("u5")) })
                TeamEntry("FS", "Felipe Santos", "Instalação", 2, { onNavigate(AppDestination.ProfileOther("u6")) })
                TeamEntry("GC", "Gabriela Costa", "Fornecedor", 3, { onNavigate(AppDestination.ProfileOther("u9")) })
                ActionRow("Convidar pessoa", LucideR.drawable.lucide_ic_user_plus, {})
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
        Box(modifier = Modifier.padding(top = 7.dp).size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(com.project.solaria_mobile.core.designsystem.theme.AppTheme.colors.secondary))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(detail, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TeamEntry(initials: String, name: String, role: String, index: Int, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(initials = initials, color = com.project.solaria_mobile.core.designsystem.components.AvatarPalette(index))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(role, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        Icon(painter = painterResource(LucideR.drawable.lucide_ic_ellipsis), contentDescription = "Mais opções de $name", tint = AppTheme.colors.onSurfaceMuted, modifier = Modifier.size(Sizing.icon))
    }
}
