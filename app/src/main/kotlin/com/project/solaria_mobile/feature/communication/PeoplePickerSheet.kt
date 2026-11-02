package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.AppCheckboxRow
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.FormField
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SheetSize
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.profile.ConnectionsViewModel

/** Título máximo de um grupo no messenger. */
internal const val MAX_GROUP_TITLE = 120

/**
 * Escolha de várias pessoas entre as conexões da conta, para criar um grupo ou acrescentar gente a ele. [exclude] são os
 * `authId` (em minúsculas) que já estão no grupo. Com [nameLabel] a folha também pede o nome do grupo, e só confirma com ele.
 */
@Composable
fun PeoplePickerSheet(
    title: String,
    confirmLabel: String,
    exclude: Set<String>,
    onConfirm: (authIds: Set<String>, name: String) -> Unit,
    onDismiss: () -> Unit,
    nameLabel: String? = null,
    viewModel: ConnectionsViewModel = hiltViewModel(key = "people-picker"),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var name by rememberSaveable { mutableStateOf("") }
    BottomSheet(onDismiss = onDismiss, scrollable = false, size = SheetSize.Expanded) {
        val dismiss = LocalSheetDismiss.current
        Column(Modifier.fillMaxSize().padding(horizontal = Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
            if (nameLabel != null) FormField(nameLabel, name, { name = it.take(MAX_GROUP_TITLE) }, maxLength = MAX_GROUP_TITLE)
            SearchField(stringResource(R.string.cn_busque_nas_suas_conexoes), query, viewModel::onQueryChange)
            UiStateHost(state, viewModel::load, modifier = Modifier.weight(1f), inline = true, loading = { ListSkeleton() }) { content ->
                // Só entra quem tem `authId`: é o identificador que o messenger conhece.
                val people = content.connections.filter { it.authId != null && it.authId.lowercase() !in exclude }
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.xxs), contentPadding = PaddingValues(bottom = Spacing.md)) {
                    if (people.isEmpty()) item { StateNotice(stringResource(R.string.gp_sem_conexoes), stringResource(R.string.gp_sem_conexoes_desc), icon = AppIcons.users) }
                    items(people, key = { it.id }) { person ->
                        val authId = person.authId.orEmpty()
                        AppCheckboxRow(person.name, authId in selected, { checked -> selected = if (checked) selected + authId else selected - authId })
                    }
                }
            }
            PrimaryButton(confirmLabel, { dismiss(); onConfirm(selected.toSet(), name.trim()) }, enabled = selected.isNotEmpty() && (nameLabel == null || name.isNotBlank()))
        }
    }
}
