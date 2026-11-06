package com.project.solaria_mobile.core.designsystem.components

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing

internal data class ErrorPresentation(val title: String, val description: String, val canRetry: Boolean = true)

internal fun ErrorKind.presentation(resources: android.content.res.Resources): ErrorPresentation = when (this) {
    ErrorKind.CONNECTIVITY -> ErrorPresentation(resources.getString(R.string.error_connectivity_title), resources.getString(R.string.error_connectivity_text))
    ErrorKind.TIMEOUT -> ErrorPresentation(resources.getString(R.string.error_timeout_title), resources.getString(R.string.error_timeout_text))
    ErrorKind.UNAUTHORIZED -> ErrorPresentation(resources.getString(R.string.error_unauthorized_title), resources.getString(R.string.error_unauthorized_text), false)
    ErrorKind.FORBIDDEN -> ErrorPresentation(resources.getString(R.string.error_forbidden_title), resources.getString(R.string.error_forbidden_text), false)
    ErrorKind.NOT_FOUND -> ErrorPresentation(resources.getString(R.string.error_not_found_title), resources.getString(R.string.error_not_found_text), false)
    ErrorKind.CONFLICT -> ErrorPresentation(resources.getString(R.string.error_conflict_title), resources.getString(R.string.error_conflict_text))
    ErrorKind.VALIDATION -> ErrorPresentation(resources.getString(R.string.error_validation_title), resources.getString(R.string.error_validation_text), false)
    ErrorKind.RATE_LIMITED -> ErrorPresentation(resources.getString(R.string.error_rate_limited_title), resources.getString(R.string.error_rate_limited_text))
    ErrorKind.NOT_SUPPORTED -> ErrorPresentation(resources.getString(R.string.error_not_supported_title), resources.getString(R.string.error_not_supported_text), false)
    ErrorKind.SERVER, ErrorKind.SERIALIZATION, ErrorKind.UNKNOWN -> ErrorPresentation(resources.getString(R.string.error_server_title), resources.getString(R.string.error_server_text))
}

@Composable
internal fun ErrorKind.presentation(): ErrorPresentation = presentation(androidx.compose.ui.platform.LocalContext.current.resources)

@Composable
fun StateMessage(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    animateBrand: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(Spacing.screen), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 360.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            BrandLoader(Modifier.size(96.dp), rotateShape = animateBrand)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(title, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.onSurface, textAlign = TextAlign.Center)
                Text(description, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.onSurfaceMuted, textAlign = TextAlign.Center)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (actionLabel != null && onAction != null) PrimaryButton(actionLabel, onAction)
                if (secondaryActionLabel != null && onSecondaryAction != null) PrimaryButton(secondaryActionLabel, onSecondaryAction, secondary = true)
                if (onBack != null) PrimaryButton(stringResource(R.string.common_back), onBack, secondary = onAction != null)
            }
        }
    }
}
