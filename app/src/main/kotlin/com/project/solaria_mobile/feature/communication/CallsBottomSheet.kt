@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
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
    loading: Boolean = false,
) {
    BottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.sm, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (loading) com.project.solaria_mobile.core.designsystem.components.ListSkeleton(rows = 3, circle = false)
            else if (failure != null) {
                val message = failure.presentation()
                StateNotice(message.title, message.description, icon = AppIcons.phone,
                    actions = if (message.canRetry) { { PrimaryButton(stringResource(R.string.cb_ver_todos_os_chamados), onOpenAll, compact = true) } } else null)
            } else {
                // Sem nenhum chamado não há o que buscar nem listar; fica só o aviso.
                if (calls.isNotEmpty() || query.isNotBlank()) {
                    SearchField(stringResource(R.string.cl_buscar_chamado), query, onQueryChange)
                    SectionHeading(stringResource(R.string.cl_recentes), faded = true, action = stringResource(R.string.hm_ver_todos), navigation = true, onAction = onOpenAll)
                }
                if (calls.isEmpty()) StateNotice(
                    if (query.isBlank()) stringResource(R.string.cb_nenhum_chamado_ainda) else stringResource(R.string.cb_nenhum_chamado_encontrado),
                    if (query.isBlank()) stringResource(R.string.cb_seus_chamados_aparecerao_aqui) else stringResource(R.string.settings_no_results_description),
                    icon = AppIcons.phone,
                    actions = if (query.isNotBlank()) { { PrimaryButton(stringResource(R.string.settings_clear_search), { onQueryChange("") }, secondary = true, compact = true) } } else null,
                )
            }
            calls.forEach { call ->
                CallCard(call, onClick = { onOpenCall(call.id) })
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}
