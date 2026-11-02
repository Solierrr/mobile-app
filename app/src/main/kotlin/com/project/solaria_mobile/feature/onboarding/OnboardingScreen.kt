@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.project.solaria_mobile.feature.onboarding

import androidx.compose.ui.platform.LocalContext
import androidx.annotation.StringRes
import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.AppDialog
import com.project.solaria_mobile.core.designsystem.components.Appear
import com.project.solaria_mobile.core.designsystem.components.ContentSkeleton
import com.project.solaria_mobile.core.designsystem.components.FormField
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.SelectableChip
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.StateNoticePage
import com.project.solaria_mobile.core.designsystem.components.StepFlow
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.address.AddressFormSection
import com.project.solaria_mobile.feature.discovery.WorkJourneyEditor
import com.project.solaria_mobile.feature.discovery.daysSummary
import com.project.solaria_mobile.feature.discovery.label
import com.project.solaria_mobile.feature.discovery.pendingDays
import com.project.solaria_mobile.feature.onboarding.domain.ProfessionalProfile
import com.project.solaria_mobile.feature.onboarding.domain.ProfileGap
import com.project.solaria_mobile.feature.onboarding.domain.Specialty

private enum class Part(@StringRes val label: Int) {
    ACTIVITY(R.string.onboarding_step_activity),
    PLACE(R.string.onboarding_step_place),
    JOURNEY(R.string.onboarding_step_journey),
    ABOUT(R.string.onboarding_step_about),
    REVIEW(R.string.onboarding_step_review),
}

/** O onboarding é obrigatório: não há como sair antes de concluir; fechar o aplicativo só guarda o rascunho. */
@Composable
fun OnboardingRoute(onDone: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.effectFlow.collect { onDone() } }
    // Fechar ou esconder o aplicativo guarda o que já foi preenchido, para retomar do mesmo ponto.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (!state.loading && state.loadFailure == null) viewModel.saveDraft() }
    val failure = state.loadFailure
    when {
        state.loading -> StateNoticePage(header = { ScreenTitle(stringResource(R.string.onboarding_title), null) }) { ContentSkeleton() }
        failure != null -> StateNoticePage(header = { ScreenTitle(stringResource(R.string.onboarding_title), null) }) {
            if (failure == ErrorKind.NOT_FOUND) {
                // Sem perfil profissional não há o que completar; quem chega aqui pelo bloqueio segue para o app.
                LaunchedEffect(Unit) { onDone() }
            } else {
                StateNotice(stringResource(R.string.onboarding_load_failed), failure.userMessage(), actions = { PrimaryButton(stringResource(R.string.common_try_again), viewModel::load, compact = true) })
            }
        }
        else -> OnboardingScreen(state, viewModel)
    }
}

/** Complete seu perfil em passos: atuação, local, jornada (só autônomos), sobre você e revisão. */
@Composable
private fun OnboardingScreen(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val parts = buildList {
        add(Part.ACTIVITY)
        add(Part.PLACE)
        if (state.independent) add(Part.JOURNEY)
        add(Part.ABOUT)
        add(Part.REVIEW)
    }
    var step by rememberSaveable { mutableIntStateOf(0) }
    // Aviso com um único OK, quando algo impede de seguir para o próximo passo.
    var notice by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    val profile = state.profile
    val resources = LocalContext.current.resources
    // O endereço só cobra os campos obrigatórios depois que a pessoa chega à revisão.
    var reviewed by rememberSaveable { mutableIntStateOf(0) }
    StepFlow(
        title = stringResource(R.string.onboarding_title),
        steps = parts.map { stringResource(it.label) },
        step = step,
        onStepChange = { step = it },
        onExit = {},
        onContinue = { current ->
            val blocked = when {
                // A apresentação cabe em 500 caracteres: passou disso, só segue depois de encurtar o texto.
                parts[current] == Part.ABOUT && profile.bioTooLong ->
                    resources.getString(R.string.onboarding_bio_limit_title) to resources.getString(R.string.onboarding_bio_limit_message, ProfessionalProfile.BIO_MAX)
                parts[current] == Part.JOURNEY && state.journey.values.none { it != null } ->
                    resources.getString(R.string.onboarding_no_day_title) to resources.getString(R.string.onboarding_no_day_message)
                parts[current] == Part.JOURNEY && pendingDays(state.journey).isNotEmpty() ->
                    resources.getString(R.string.onboarding_no_hours_title) to resources.getString(R.string.onboarding_no_hours_message)
                else -> null
            }
            if (blocked != null) {
                notice = blocked
            } else {
                viewModel.saveDraft()
            }
            blocked == null
        },
        finishLabel = stringResource(R.string.onboarding_finish),
        onFinish = viewModel::complete,
        busy = state.saving,
        canFinish = state.canComplete,
        stepTitles = true,
        showStepLabel = false,
        bottomBack = false,
        canExit = false,
        footer = {
            state.failure?.let { Text(it.userMessage(), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium) }
        },
    ) { current ->
        when (parts[current]) {
            Part.ACTIVITY -> ActivityStep(profile, viewModel)
            Part.PLACE -> PlaceStep(profile, reviewed > 0, viewModel)
            Part.JOURNEY -> WorkJourneyEditor(state.journey, viewModel::updateJourney)
            Part.ABOUT -> AboutStep(profile, viewModel)
            Part.REVIEW -> {
                LaunchedEffect(Unit) { reviewed = 1 }
                ReviewStep(state, parts) { step = it }
            }
        }
    }
    notice?.let { (title, message) ->
        AppDialog(
            onDismissRequest = { notice = null },
            title = title,
            message = message,
            confirmLabel = stringResource(R.string.onboarding_ok),
            dismissLabel = null,
            onConfirm = { notice = null },
        )
    }
}

@Composable
private fun ActivityStep(profile: ProfessionalProfile, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(R.string.onboarding_activity_question), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Specialty.entries.forEach { specialty ->
                SelectableChip(stringResource(specialty.labelRes()), specialty in profile.specialties, {
                    viewModel.update { p -> p.copy(specialties = if (specialty in p.specialties) p.specialties - specialty else p.specialties + specialty) }
                })
            }
        }
    }
    // "Outros" pede para dizer o que a pessoa faz; só depois (ou, sem "Outros", logo ao escolher algo) vem a experiência.
    if (Specialty.OTHER in profile.specialties) {
        Appear {
            FormField(
                label = stringResource(R.string.onboarding_other_label),
                value = profile.otherSpecialty,
                onValueChange = { value -> viewModel.update { it.copy(otherSpecialty = value) } },
                maxLength = ProfessionalProfile.OTHER_MAX,
            )
        }
    }
    if (profile.specialties.isNotEmpty() && (Specialty.OTHER !in profile.specialties || profile.otherSpecialty.isNotBlank())) {
        Appear { Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(stringResource(R.string.onboarding_experience_question), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
            FormField(
                label = stringResource(R.string.onboarding_experience_label),
                value = profile.yearsExperience?.toString().orEmpty(),
                onValueChange = { raw -> viewModel.update { p -> p.copy(yearsExperience = raw.filter(Char::isDigit).toIntOrNull()?.coerceAtMost(ProfessionalProfile.EXPERIENCE_MAX)) } },
                keyboardType = KeyboardType.Number,
                maxLength = 2,
                imeAction = ImeAction.Done,
            )
        } }
    }
}

@Composable
private fun PlaceStep(profile: ProfessionalProfile, showErrors: Boolean, viewModel: OnboardingViewModel) {
    Text(stringResource(R.string.onboarding_place_question), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
    AddressFormSection(
        address = profile.address,
        onChange = { transform -> viewModel.update { it.copy(address = transform(it.address)) } },
        errors = if (showErrors) profile.address.validate() else emptySet(),
        radiusKm = profile.serviceRadiusKm,
        onRadiusChange = { km -> viewModel.update { it.copy(serviceRadiusKm = km) } },
    )
}

@Composable
private fun AboutStep(profile: ProfessionalProfile, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(R.string.onboarding_about_question), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
        FormField(
            label = stringResource(R.string.onboarding_about_label),
            value = profile.bio,
            onValueChange = { value -> viewModel.update { it.copy(bio = value) } },
            singleLine = false,
            minLines = 5,
            maxLength = ProfessionalProfile.BIO_INPUT_MAX,
        )
    }
}

/** Resumo antes de concluir; cada bloco leva de volta ao passo para corrigir. */
@Composable
private fun ReviewStep(state: OnboardingUiState, parts: List<Part>, goTo: (Int) -> Unit) {
    val profile = state.profile
    val gaps = profile.gaps()
    val resources = LocalContext.current.resources
    fun indexOf(part: Part) = parts.indexOf(part)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeading(stringResource(R.string.onboarding_step_activity), faded = true)
        ActionRow(
            title = profile.specialties.sortedBy { it.ordinal }
                .joinToString(", ") { if (it == Specialty.OTHER && profile.otherSpecialty.isNotBlank()) profile.otherSpecialty.trim() else resources.getString(it.labelRes()) }
                .ifBlank { stringResource(R.string.onboarding_review_no_specialty) },
            icon = AppIcons.wrench, onClick = { goTo(indexOf(Part.ACTIVITY)) }, trailing = stringResource(R.string.common_edit),
        )
        if (ProfileGap.SPECIALTIES in gaps) Text(stringResource(R.string.onboarding_review_pick_specialty), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
        if (ProfileGap.OTHER_SPECIALTY in gaps) Text(stringResource(R.string.onboarding_review_describe_other), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
        if (ProfileGap.EXPERIENCE in gaps) Text(stringResource(R.string.onboarding_review_experience_missing), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeading(stringResource(R.string.onboarding_step_place), faded = true)
        ActionRow(
            title = profile.address.streetLine().ifBlank { stringResource(R.string.onboarding_review_no_address) },
            icon = AppIcons.map_pin, onClick = { goTo(indexOf(Part.PLACE)) }, trailing = stringResource(R.string.common_edit),
            subtitle = listOfNotNull(profile.address.placeLine().ifBlank { null }, profile.serviceRadiusKm?.let { stringResource(R.string.onboarding_review_radius, it) }).joinToString(" · ").ifBlank { null },
        )
        if (ProfileGap.ADDRESS in gaps) Text(stringResource(R.string.onboarding_review_address_missing), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
    }
    if (state.independent) Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeading(stringResource(R.string.onboarding_step_journey), faded = true)
        ActionRow(
            title = state.journey.daysSummary().label().ifBlank { stringResource(R.string.onboarding_no_day_title) },
            icon = AppIcons.cloud_sun, onClick = { goTo(indexOf(Part.JOURNEY)) }, trailing = stringResource(R.string.common_edit),
        )
        if (pendingDays(state.journey).isNotEmpty()) Text(stringResource(R.string.onboarding_review_hours_missing), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeading(stringResource(R.string.onboarding_step_about), faded = true)
        ActionRow(
            title = profile.bio.trim().ifBlank { stringResource(R.string.onboarding_review_no_bio) }, icon = AppIcons.user_round_check,
            onClick = { goTo(indexOf(Part.ABOUT)) }, trailing = stringResource(R.string.common_edit),
        )
    }
}
