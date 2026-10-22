@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.presentation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.Spacing

@Composable
fun CallsBottomSheet(
    calls: List<ServiceCall>,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenCall: (String) -> Unit,
    onOpenAll: () -> Unit,
    failure: ErrorKind? = null,
) {
    BottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ScreenTitle("Chamados", onBack = onDismiss)
            if (failure != null) {
                val message = failure.presentation()
                StateNotice(message.title, message.description, icon = AppIcons.phone,
                    actions = if (message.canRetry) { { PrimaryButton("Ver todos os chamados", onOpenAll, compact = true) } } else null)
            } else {
                SearchField("Buscar chamado", query, onQueryChange)
                SectionHeading("Recentes", faded = true, action = "Ver todos", navigation = true, onAction = onOpenAll)
                if (calls.isEmpty()) StateNotice(
                    if (query.isBlank()) "Nenhum chamado ainda" else "Nenhum chamado encontrado",
                    if (query.isBlank()) "Seus chamados aparecerão aqui." else "Tente outro termo ou limpe a busca.",
                    icon = AppIcons.phone,
                    actions = if (query.isNotBlank()) { { PrimaryButton("Limpar busca", { onQueryChange("") }, secondary = true, compact = true) } } else null,
                )
            }
            calls.forEach { call ->
                CallCard(call, onClick = { onOpenCall(call.id) })
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}
