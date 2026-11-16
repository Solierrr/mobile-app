package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing

@Composable
fun StateNotice(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = AppIcons.info,
    actions: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 320.dp).fillMaxWidth().padding(vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.padding(bottom = Spacing.md).size(Sizing.stateIconContainer).background(AppTheme.colors.inputFill, RoundedCornerShape(Radius.large)), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurface)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.onSurface, textAlign = TextAlign.Center)
                Text(description, Modifier.widthIn(max = 285.dp), style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.onSurfaceMuted, textAlign = TextAlign.Center)
            }
            if (actions != null) Box(Modifier.padding(top = Spacing.action), contentAlignment = Alignment.Center) { actions() }
        }
    }
}

@Composable
fun StateNoticePage(header: (@Composable () -> Unit)?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().padding(Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.section)) {
        header?.invoke()
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.fillMaxWidth().padding(bottom = Sizing.stateIconContainer), contentAlignment = Alignment.Center) { content() }
            }
        }
    }
}
