@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.data.ServiceCall
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

@Composable
fun CallsBottomSheet(
    calls: List<ServiceCall>,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenCall: () -> Unit,
    onOpenAll: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SolariaWhite,
        shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ScreenTitle("Chamados", onBack = onDismiss, trailingIcon = LucideR.drawable.lucide_ic_sliders_horizontal, trailingLabel = "Filtrar", onTrailingClick = {})
            SearchField("Buscar chamado", query, onQueryChange, trailingIcon = LucideR.drawable.lucide_ic_sliders_horizontal, onTrailingClick = {})
            SectionHeading("Recentes", faded = true, action = "Ver todos", onAction = onOpenAll)
            calls.forEach { call ->
                CallCard(call, onClick = onOpenCall)
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}
