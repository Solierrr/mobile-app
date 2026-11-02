package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.ContentSkeleton
import com.project.solaria_mobile.core.designsystem.components.FormField
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.StateNoticePage
import com.project.solaria_mobile.core.designsystem.components.StepFlow
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.address.AddressFormSection
import com.project.solaria_mobile.feature.address.domain.AddressField
import com.project.solaria_mobile.feature.project.domain.NewProjectDraft
import com.project.solaria_mobile.feature.project.domain.NewProjectField

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
        StateNoticePage(header = { ScreenTitle(stringResource(R.string.pd_editar_projeto), onBack) }) { ContentSkeleton() }
    } else if (state.editing && loadFailure != null && state.draft.name.isBlank()) {
        // Sem os dados atuais não há o que editar: mostra a falha com nova tentativa, em vez de um formulário vazio.
        StateNoticePage(header = { ScreenTitle(stringResource(R.string.pd_editar_projeto), onBack) }) {
            StateNotice(stringResource(R.string.np_nao_foi_possivel_abrir_o), loadFailure.userMessage(), actions = {
                PrimaryButton(stringResource(R.string.common_try_again), viewModel::loadDraft, compact = true)
            })
        }
    } else NewProjectScreen(state, onBack, viewModel::update, viewModel::submit, viewModel::validateStep)
}

@Composable
private fun steps() = listOf(stringResource(R.string.np_dados_do_projeto), stringResource(R.string.onboarding_step_place), stringResource(R.string.onboarding_step_review))
private const val STEP_DATA = 0
private const val STEP_PLACE = 1

/** Criar e editar projeto em três passos: dados, local (CEP preenche o endereço e mostra no mapa) e revisão. */
@Composable
fun NewProjectScreen(
    state: NewProjectUiState,
    onBack: () -> Unit,
    onChange: ((NewProjectDraft) -> NewProjectDraft) -> Unit,
    onSubmit: () -> Unit,
    onValidateStep: (Int) -> Boolean = { true },
) {
    val draft = state.draft
    val enabled = !state.submitting
    var step by rememberSaveable { mutableIntStateOf(STEP_DATA) }
    val addressErrors = state.errors.mapNotNull { it.toAddressField() }.toSet()
    StepFlow(
        title = if (state.editing) stringResource(R.string.pd_editar_projeto) else stringResource(R.string.pd_novo_projeto),
        steps = steps(),
        step = step,
        onStepChange = { step = it },
        onExit = onBack,
        onContinue = onValidateStep,
        finishLabel = when {
            state.editing && state.submitting -> stringResource(R.string.ps_salvando)
            state.editing -> stringResource(R.string.np_salvar_alteracoes)
            state.submitting -> stringResource(R.string.np_criando)
            else -> stringResource(R.string.pl_criar_projeto)
        },
        onFinish = onSubmit,
        busy = state.submitting,
        canFinish = state.online,
        confirmExit = !state.editing && (draft.name.isNotBlank() || draft.description.isNotBlank() || !draft.address.isBlank),
        footer = {
            state.failure?.let { kind -> Text(kind.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }
            if (!state.online) Text(
                if (state.editing) stringResource(R.string.np_conecte_se_para_salvar_suas) else stringResource(R.string.np_conecte_se_para_criar_seu),
                color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium,
            )
        },
    ) { current ->
        when (current) {
            STEP_DATA -> {
                FormField(
                    label = stringResource(R.string.np_nome_do_projeto),
                    value = draft.name,
                    onValueChange = { value -> onChange { it.copy(name = value) } },
                    error = if (NewProjectField.NAME in state.errors) "Informe um nome com pelo menos ${NewProjectDraft.NAME_MIN} letras." else null,
                    maxLength = NewProjectDraft.NAME_MAX,
                    enabled = enabled,
                )
                FormField(
                    label = stringResource(R.string.ps_descricao_opcional),
                    value = draft.description,
                    onValueChange = { value -> onChange { it.copy(description = value) } },
                    error = if (NewProjectField.DESCRIPTION in state.errors) stringResource(R.string.np_a_descricao_e_longa_demais) else null,
                    singleLine = false,
                    minLines = 3,
                    maxLength = NewProjectDraft.DESCRIPTION_MAX,
                    enabled = enabled,
                )
            }
            STEP_PLACE -> AddressFormSection(
                address = draft.address,
                onChange = { transform -> onChange { it.withAddress(transform(it.address)) } },
                errors = addressErrors,
                enabled = enabled,
            )
            else -> ReviewStep(draft, state.errors, enabled) { step = it }
        }
    }
}

/** Resumo antes de criar; cada bloco leva de volta ao passo para corrigir. */
@Composable
private fun ReviewStep(draft: NewProjectDraft, errors: Set<NewProjectField>, enabled: Boolean, goTo: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeading(stringResource(R.string.np_dados_do_projeto), faded = true)
        ActionRow(
            title = draft.name.ifBlank { stringResource(R.string.np_sem_nome) }, icon = AppIcons.folder, onClick = { goTo(STEP_DATA) }, enabled = enabled,
            subtitle = draft.description.trim().ifBlank { "Sem descrição" }, trailing = stringResource(R.string.common_edit),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeading(stringResource(R.string.onboarding_step_place), faded = true)
        ActionRow(
            title = draft.address.streetLine().ifBlank { stringResource(R.string.onboarding_review_no_address) }, icon = AppIcons.map_pin, onClick = { goTo(STEP_PLACE) }, enabled = enabled,
            subtitle = draft.address.placeLine().ifBlank { null }, trailing = stringResource(R.string.common_edit),
        )
        if (errors.any { it.toAddressField() != null } || draft.address.validate().isNotEmpty()) {
            Text(stringResource(R.string.np_falta_completar_o_endereco_para), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun NewProjectField.toAddressField(): AddressField? = when (this) {
    NewProjectField.ZIP -> AddressField.ZIP
    NewProjectField.STREET -> AddressField.STREET
    NewProjectField.NUMBER -> AddressField.NUMBER
    NewProjectField.NEIGHBORHOOD -> AddressField.NEIGHBORHOOD
    NewProjectField.CITY -> AddressField.CITY
    NewProjectField.STATE -> AddressField.STATE
    else -> null
}
