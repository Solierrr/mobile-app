package com.project.solaria_mobile.core.designsystem.components

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing

/**
 * Barra de progresso segmentada, um traço por passo. Os passos já visitados ([furthest]) podem ser tocados para voltar
 * ou avançar até onde a pessoa já chegou.
 */
@Composable
fun StepProgress(step: Int, total: Int, furthest: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val stepFormat = stringResource(R.string.step_flow_step_of)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        repeat(total) { index ->
            val reachable = index <= furthest
            Box(
                Modifier.weight(1f).height(STEP_TOUCH_HEIGHT)
                    .semantics { contentDescription = stepFormat.format(index + 1, total); selected = index == step }
                    .then(if (reachable && index != step) Modifier.clickable(role = Role.Button) { onSelect(index) } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(STEP_BAR_HEIGHT).clip(RoundedCornerShape(Radius.pill))
                        .background(if (index <= step) AppTheme.colors.action else AppTheme.colors.outline),
                )
            }
        }
    }
}

/**
 * Fluxo em passos para formulários longos. A tela inteira rola, cabeçalho e progresso junto com o conteúdo do passo; só os
 * botões de baixo ficam fixos. O último passo troca "Continuar" por [finishLabel], e [onContinue] valida o passo antes de
 * avançar.
 *
 * - [stepTitles]: o título da tela é o nome do passo em vez de [title].
 * - [showStepLabel]: mostra "Passo X de N · nome" sob a barra.
 * - [bottomBack]: mostra o botão "Voltar" embaixo; sem ele, só a seta do topo volta.
 * - [canExit]: permite sair no primeiro passo. Sem isso, o primeiro passo não tem seta nem aceita o botão voltar do
 *   sistema (fluxos obrigatórios); a partir do segundo, a seta volta um passo.
 * - [confirmExit]: ao sair no primeiro passo, pede confirmação para não perder o que foi digitado.
 */
@Composable
fun StepFlow(
    title: String,
    steps: List<String>,
    step: Int,
    onStepChange: (Int) -> Unit,
    onExit: () -> Unit,
    onContinue: (Int) -> Boolean,
    finishLabel: String,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    canFinish: Boolean = true,
    confirmExit: Boolean = false,
    stepTitles: Boolean = false,
    showStepLabel: Boolean = true,
    bottomBack: Boolean = true,
    canExit: Boolean = true,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.(Int) -> Unit,
) {
    var furthest by rememberSaveable { mutableIntStateOf(step) }
    LaunchedEffect(step) { if (step > furthest) furthest = step }
    var confirming by rememberSaveable { mutableStateOf(false) }
    val last = step == steps.lastIndex
    val back: () -> Unit = {
        when {
            busy -> Unit
            step > 0 -> onStepChange(step - 1)
            !canExit -> Unit
            confirmExit -> confirming = true
            else -> onExit()
        }
    }
    BackHandler(onBack = back)
    // O rodapé flutua sem fundo sobre o conteúdo, como a barra de navegação; o conteúdo reserva o espaço dele no fim.
    var footerHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Box(modifier.fillMaxSize().background(AppTheme.colors.surface).imePadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.screenTop),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                ScreenTitle(if (stepTitles) steps[step] else title, if (step > 0 || canExit) back else null)
                StepProgress(step, steps.size, furthest, { if (!busy) onStepChange(it) })
                if (showStepLabel) Text(
                    stringResource(R.string.step_flow_step_label, step + 1, steps.size, steps[step]),
                    color = AppTheme.colors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) { content(step) }
            Spacer(Modifier.height(footerHeight))
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .onSizeChanged { footerHeight = with(density) { it.height.toDp() } }
                .padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            footer?.invoke(this)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                if (bottomBack && step > 0) AppButton(stringResource(R.string.common_back), { onStepChange(step - 1) }, Modifier.weight(1f), variant = ButtonVariant.Secondary, enabled = !busy)
                AppButton(
                    text = if (last) finishLabel else stringResource(R.string.common_continue),
                    onClick = { if (last) onFinish() else if (onContinue(step)) onStepChange(step + 1) },
                    modifier = Modifier.weight(1f),
                    enabled = !last || canFinish,
                    loading = busy,
                )
            }
        }
    }
    if (confirming) AppDialog(
        onDismissRequest = { confirming = false },
        title = stringResource(R.string.step_flow_exit_title),
        message = stringResource(R.string.step_flow_exit_message),
        confirmLabel = stringResource(R.string.step_flow_exit_confirm),
        dismissLabel = stringResource(R.string.step_flow_exit_dismiss),
        onConfirm = { confirming = false; onExit() },
        destructive = true,
    )
}

private val STEP_TOUCH_HEIGHT = 24.dp
private val STEP_BAR_HEIGHT = 6.dp
