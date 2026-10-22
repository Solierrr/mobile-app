@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Elevation
import com.project.solaria_mobile.core.designsystem.theme.Radius

@Composable
fun BottomSheet(
    onDismiss: () -> Unit,
    state: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
        containerColor = AppTheme.colors.surface,
        tonalElevation = Elevation.flat,
        scrimColor = AppTheme.colors.onSurface.copy(alpha = 0.18f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppTheme.colors.onSurfaceSubtle) },
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.85f).dp)
                .imePadding().navigationBarsPadding().then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            content = content,
        )
    }
}
