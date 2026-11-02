package com.project.solaria_mobile.core.designsystem.components

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.offline.LocalOffline

@Composable
fun RefreshButton(
    onClick: () -> Unit,
    refreshing: Boolean = false,
    label: String = stringResource(R.string.rf_atualizar),
    modifier: Modifier = Modifier,
    background: Color = AppTheme.colors.inputFill,
) {
    val offline = LocalOffline.current
    val reducedMotion = Settings.Global.getFloat(LocalContext.current.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    val rotation = if (refreshing && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "refresh")
        val angle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "rotation")
        angle
    } else 0f
    val state = stringResource(
        when {
            refreshing -> R.string.rf_atualizando
            offline -> R.string.rf_sem_conexao
            else -> R.string.rf_pronto_para_atualizar
        },
    )
    RoundIconButton(
        AppIcons.refresh, label, onClick,
        modifier.rotate(rotation).semantics { stateDescription = state },
        background = background,
        enabled = !refreshing && !offline,
    )
}
