@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite
import kotlinx.coroutines.launch

@Composable
fun EnterpriseScreen(onBack: () -> Unit) {
    var infoOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(SolariaWhite).verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().height(255.dp)) {
            Image(painter = painterResource(R.drawable.project_building), contentDescription = "Google", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(LucideR.drawable.lucide_ic_chevron_left, "Voltar", onBack)
                RoundIconButton(LucideR.drawable.lucide_ic_share, "Compartilhar", {})
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.lg, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(Radius.medium), color = SolariaWhite, shadowElevation = 2.dp) {
                    Image(painter = painterResource(R.drawable.company_logo_1), contentDescription = null, contentScale = ContentScale.Crop)
                }
                Text("Google", color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            }
            ProfileField("Unidade principal", "São Paulo")
            ProfileField("Tipo de empresa", "BigTech")
            ProfileField("Telefone", "55+ (11) 91022–3438")
            SectionHeading("Opções", faded = true)
            ActionRow("Informações", LucideR.drawable.lucide_ic_info, { infoOpen = true })
            ActionRow("Compartilhar", LucideR.drawable.lucide_ic_share, {})
        }
    }

    if (infoOpen) {
        ModalBottomSheet(
            onDismissRequest = { infoOpen = false },
            sheetState = sheetState,
            containerColor = SolariaWhite,
            shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
            dragHandle = null,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ScreenTitle("Informações", onBack = { scope.launch { sheetState.hide(); infoOpen = false } })
                Text("Google", color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Text("Perfil público da empresa", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                ProfileField("Unidade principal", "São Paulo")
                ProfileField("Tipo de empresa", "BigTech")
                ProfileField("Telefone", "55+ (11) 91022–3438")
                ActionRow("Compartilhar", LucideR.drawable.lucide_ic_share, {})
            }
        }
    }
}
