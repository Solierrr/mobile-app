package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.SelectableChip
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing

/**
 * Nota de 0 a 10 para a sessão do assistente, pedida ao sair dela. [onSubmit] recebe a nota; [onDismiss] roda quando a folha
 * se fecha, enviando ou não ("agora não" e fechar valem o mesmo): a pessoa segue para onde ia.
 */
@Composable
fun NpsSheet(onSubmit: (Int) -> Unit, onDismiss: () -> Unit) {
    var score by remember { mutableStateOf<Int?>(null) }
    BottomSheet(onDismiss = onDismiss) {
        val dismiss = LocalSheetDismiss.current
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(stringResource(R.string.nps_titulo), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.nps_desc), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
            listOf(0..5, 6..10).forEach { range ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    range.forEach { value -> SelectableChip(value.toString(), score == value, { score = value }, Modifier.weight(1f)) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.nps_pouco), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.nps_muito), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
            }
            PrimaryButton(stringResource(R.string.nps_enviar), { score?.let(onSubmit); dismiss() }, enabled = score != null)
            PrimaryButton(stringResource(R.string.nps_agora_nao), { dismiss() }, secondary = true, compact = true)
        }
    }
}
