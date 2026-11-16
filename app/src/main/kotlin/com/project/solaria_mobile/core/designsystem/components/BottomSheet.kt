@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Elevation
import com.project.solaria_mobile.core.designsystem.theme.ProvideSurfaceContainers
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.ScrollState

/**
 * Tamanhos das sheets. Compacto acompanha o conteúdo, para menus curtos; conteúdo cresce até o limite e rola;
 * expandido ocupa a altura útil desde o início, para listas longas como a galeria.
 */
enum class SheetSize { Compact, Content, Expanded }

private const val CONTENT_MAX_FRACTION = 0.85f
private const val EXPANDED_FRACTION = 0.9f

@Composable
fun BottomSheet(
    onDismiss: () -> Unit,
    state: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    scrollable: Boolean = true,
    size: SheetSize = SheetSize.Content,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val heightModifier = when (size) {
        SheetSize.Compact -> Modifier
        SheetSize.Content -> Modifier.heightIn(max = (screenHeight * CONTENT_MAX_FRACTION).dp)
        SheetSize.Expanded -> Modifier.height((screenHeight * EXPANDED_FRACTION).dp)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
        containerColor = AppTheme.colors.surface,
        tonalElevation = Elevation.flat,
        scrimColor = AppTheme.colors.onSurface.copy(alpha = 0.18f),
        dragHandle = {
            Box(
                Modifier.padding(vertical = Spacing.xs).width(64.dp).height(4.dp)
                    .clip(RoundedCornerShape(Radius.pill)).background(AppTheme.colors.onSurfaceSubtle),
            )
        },
    ) {
        ProvideSurfaceContainers {
            Column(
                Modifier.fillMaxWidth().then(heightModifier)
                    .imePadding().navigationBarsPadding().then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier),
                content = content,
            )
        }
    }
}
