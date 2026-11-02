package com.project.solaria_mobile.core.designsystem.components

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import java.time.LocalTime
import kotlin.math.abs

/**
 * Rodas de hora e minuto no estilo do seletor do iOS: a linha do meio é a escolhida, as vizinhas esmaecem e o gesto
 * encaixa no valor mais próximo. [onChange] é chamado a cada valor que passa pelo meio.
 */
@Composable
fun TimeWheel(time: LocalTime, onChange: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    var hour by remember { mutableIntStateOf(time.hour) }
    var minute by remember { mutableIntStateOf(time.minute) }
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Wheel(HOURS, hour, stringResource(R.string.time_hour)) { hour = it; onChange(LocalTime.of(hour, minute)) }
        Text(":", Modifier.padding(horizontal = Spacing.xxs), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
        Wheel(MINUTES, minute, stringResource(R.string.time_minute)) { minute = it; onChange(LocalTime.of(hour, minute)) }
    }
}

@Composable
private fun Wheel(count: Int, initial: Int, label: String, onSelect: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    val fling = rememberSnapFlingBehavior(state, SnapPosition.Center)
    val current by rememberUpdatedState(onSelect)
    val centered by remember { derivedStateOf { centeredIndex(state) } }
    val descriptionFormat = stringResource(R.string.time_wheel_description)
    LaunchedEffect(state) { snapshotFlow { centeredIndex(state) }.collect { current(it) } }
    Box(
        Modifier.width(WHEEL_WIDTH).height(ITEM_HEIGHT * VISIBLE_ITEMS).semantics { contentDescription = descriptionFormat.format(label, "%02d".format(centered)) },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxWidth().height(ITEM_HEIGHT).clip(RoundedCornerShape(Radius.medium)).background(AppTheme.colors.inputFill))
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = ITEM_HEIGHT * (VISIBLE_ITEMS / 2)),
            modifier = Modifier.fillMaxSize(),
        ) {
            items((0 until count).toList()) { value ->
                Box(Modifier.fillMaxWidth().height(ITEM_HEIGHT), contentAlignment = Alignment.Center) {
                    Text(
                        "%02d".format(value),
                        Modifier.alpha(if (value == centered) 1f else UNSELECTED_ALPHA),
                        color = AppTheme.colors.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

/** O item cujo centro está mais perto do centro da roda. */
private fun centeredIndex(state: LazyListState): Int {
    val info = state.layoutInfo
    val center = info.viewportStartOffset + (info.viewportEndOffset - info.viewportStartOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: 0
}

/** Seletor de horário (24 h) numa sheet, com as rodas do [TimeWheel]. Quem chama fecha a sheet ao receber [onConfirm]. */
@Composable
fun AppTimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.common_confirm),
    title: String = stringResource(R.string.time_title),
) {
    var time by remember { mutableStateOf(initial) }
    BottomSheet(onDismiss = onDismiss, scrollable = false, size = SheetSize.Content) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, Modifier.fillMaxWidth(), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
            TimeWheel(time, { time = it })
            AppButton(confirmLabel, { onConfirm(time) })
        }
    }
}

/**
 * Início e fim de um período numa só sheet, com uma roda para cada. O fim precisa ser depois do início; enquanto não for,
 * o botão fica desabilitado e a sheet explica. Sem [initial], começa em 08:00 às 17:00. Quem chama fecha a sheet ao
 * receber [onConfirm].
 */
@Composable
fun AppTimeRangePickerDialog(
    initial: Pair<LocalTime, LocalTime>?,
    onConfirm: (LocalTime, LocalTime) -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
    confirmLabel: String = stringResource(R.string.common_confirm),
    /** Com ele, a sheet ganha um segundo botão que aplica o mesmo horário a todos os dias, e [confirmLabel] vale só para o dia aberto. */
    onConfirmAll: ((LocalTime, LocalTime) -> Unit)? = null,
    confirmAllLabel: String = stringResource(R.string.time_apply_all),
) {
    var start by remember { mutableStateOf(initial?.first ?: LocalTime.of(8, 0)) }
    var end by remember { mutableStateOf(initial?.second ?: LocalTime.of(17, 0)) }
    val valid = end.isAfter(start)
    BottomSheet(onDismiss = onDismiss, scrollable = false, size = SheetSize.Content) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            title?.let { Text(it, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(stringResource(R.string.time_start), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelLarge)
                    TimeWheel(start, { start = it })
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(stringResource(R.string.time_end), color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelLarge)
                    TimeWheel(end, { end = it })
                }
            }
            if (!valid) Text(stringResource(R.string.time_end_before_start), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodyMedium)
            AppButton(if (onConfirmAll != null) stringResource(R.string.time_apply_day) else confirmLabel, { onConfirm(start, end) }, enabled = valid)
            onConfirmAll?.let { all -> AppButton(confirmAllLabel, { all(start, end) }, variant = ButtonVariant.Secondary, enabled = valid) }
        }
    }
}

private const val HOURS = 24
private const val MINUTES = 60
private const val VISIBLE_ITEMS = 5
private const val UNSELECTED_ALPHA = 0.3f
private val ITEM_HEIGHT = 44.dp
private val WHEEL_WIDTH = 56.dp
