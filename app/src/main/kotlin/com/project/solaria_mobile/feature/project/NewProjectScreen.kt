package com.project.solaria_mobile.feature.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.designsystem.components.FormField
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.project.domain.NewProjectDraft
import com.project.solaria_mobile.feature.project.domain.NewProjectField
import com.project.solaria_mobile.core.designsystem.components.StateNoticePage
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.ContentSkeleton

@Composable
fun NewProjectRoute(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    onSaved: (String) -> Unit = { onBack() },
    viewModel: NewProjectViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effectFlow.collect { effect ->
            when (effect) {
                is NewProjectEffect.Created -> onCreated(effect.projectId)
                is NewProjectEffect.Saved -> onSaved(effect.projectId)
            }
        }
    }
    val loadFailure = state.failure
    if (state.loading) {
        StateNoticePage(header = { ScreenTitle("Editar projeto", onBack) }) { ContentSkeleton() }
    } else if (state.editing && loadFailure != null && state.draft.name.isBlank()) {
        // Sem os dados atuais não há o que editar: mostra a falha com nova tentativa, em vez de um formulário vazio.
        StateNoticePage(header = { ScreenTitle("Editar projeto", onBack) }) {
            StateNotice("Não foi possível abrir o projeto", loadFailure.userMessage(), actions = {
                PrimaryButton("Tentar novamente", viewModel::loadDraft, compact = true)
            })
        }
    } else NewProjectScreen(state, onBack, viewModel::update, viewModel::submit)
}

@Composable
fun NewProjectScreen(
    state: NewProjectUiState,
    onBack: () -> Unit,
    onChange: ((NewProjectDraft) -> NewProjectDraft) -> Unit,
    onSubmit: () -> Unit,
) {
    val draft = state.draft
    val enabled = !state.submitting
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.surface)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.screenTop, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenTitle(if (state.editing) "Editar projeto" else "Novo projeto", onBack)
        SectionHeading("Dados do projeto", faded = true)
        FormField(
            label = "Nome do projeto",
            value = draft.name,
            onValueChange = { value -> onChange { it.copy(name = value) } },
            error = if (NewProjectField.NAME in state.errors) "Informe um nome com pelo menos ${NewProjectDraft.NAME_MIN} letras." else null,
            maxLength = NewProjectDraft.NAME_MAX,
            enabled = enabled,
        )
        FormField(
            label = "Descrição (opcional)",
            value = draft.description,
            onValueChange = { value -> onChange { it.copy(description = value) } },
            error = if (NewProjectField.DESCRIPTION in state.errors) "A descrição é longa demais." else null,
            singleLine = false,
            minLines = 3,
            maxLength = NewProjectDraft.DESCRIPTION_MAX,
            enabled = enabled,
        )
        SectionHeading("Local (opcional)", faded = true)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
            FormField(
                label = "UF",
                value = draft.state,
                onValueChange = { value -> onChange { it.copy(state = value.uppercase().filter { c -> c.isLetter() }) } },
                modifier = Modifier.weight(0.35f),
                error = if (NewProjectField.STATE in state.errors) "UF inválida." else null,
                maxLength = 2,
                enabled = enabled,
            )
            FormField(
                label = "Cidade",
                value = draft.city,
                onValueChange = { value -> onChange { it.copy(city = value) } },
                modifier = Modifier.weight(0.65f),
                error = if (NewProjectField.CITY in state.errors) "Nome longo demais." else null,
                maxLength = NewProjectDraft.CITY_MAX,
                enabled = enabled,
            )
        }
        FormField(
            label = "Bairro",
            value = draft.neighborhood,
            onValueChange = { value -> onChange { it.copy(neighborhood = value) } },
            maxLength = 120,
            enabled = enabled,
        )
        FormField(
            label = "CEP",
            value = draft.zip,
            onValueChange = { value -> onChange { it.copy(zip = value.filter { c -> c.isDigit() || c == '-' }) } },
            error = if (NewProjectField.ZIP in state.errors) "O CEP tem 8 números." else null,
            keyboardType = KeyboardType.Number,
            maxLength = 9,
            enabled = enabled,
        )
        FormField(
            label = "Rua",
            value = draft.street,
            onValueChange = { value -> onChange { it.copy(street = value) } },
            maxLength = 160,
            enabled = enabled,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
            FormField(
                label = "Número",
                value = draft.number,
                onValueChange = { value -> onChange { it.copy(number = value) } },
                modifier = Modifier.weight(0.4f),
                maxLength = 20,
                enabled = enabled,
            )
            FormField(
                label = "Complemento",
                value = draft.complement,
                onValueChange = { value -> onChange { it.copy(complement = value) } },
                modifier = Modifier.weight(0.6f),
                maxLength = 120,
                imeAction = ImeAction.Done,
                enabled = enabled,
            )
        }
        state.failure?.let { kind ->
            Text(kind.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton(
            text = when {
                state.editing && state.submitting -> "Salvando…"
                state.editing -> "Salvar alterações"
                state.submitting -> "Criando…"
                else -> "Criar projeto"
            },
            onClick = onSubmit,
            enabled = enabled && state.online,
        )
        if (!state.online) Text(if (state.editing) "Conecte-se para salvar. Suas alterações permanecem nesta tela." else "Conecte-se para criar. Seu rascunho permanece nesta tela.", color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
    }
}
