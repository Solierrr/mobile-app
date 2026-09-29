@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.MapBackdrop
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaMapPlaceholder
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

@Composable
fun CallDetailsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(SolariaWhite).verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(340.dp).background(SolariaMapPlaceholder)) {
            MapBackdrop(modifier = Modifier.fillMaxSize())
            RoundIconButton(
                icon = LucideR.drawable.lucide_ic_chevron_left,
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
                ActionRow("Google Maps", LucideR.drawable.lucide_ic_map_pin, {}, compact = true, modifier = Modifier.weight(1f))
                ActionRow("Copiar", LucideR.drawable.lucide_ic_copy, {}, compact = true, modifier = Modifier.weight(1f))
            }
            SectionHeading("Opções", faded = true)
            ActionRow("Marcar como resolvido", LucideR.drawable.lucide_ic_check, {})
            ActionRow("Entrar em contato", LucideR.drawable.lucide_ic_message_circle, {})
            ActionRow("Denunciar", LucideR.drawable.lucide_ic_shield_alert, {})
        }
    }
}
