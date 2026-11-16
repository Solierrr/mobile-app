package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.project.solaria_mobile.core.designsystem.theme.actionGradient
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLength: Int = Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = singleLine,
            minLines = minLines,
            enabled = enabled,
            isError = error != null,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.medium),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.primary,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedContainerColor = AppTheme.colors.inputFill,
                unfocusedContainerColor = AppTheme.colors.inputFill,
                focusedLabelColor = AppTheme.colors.primary,
                errorBorderColor = AppTheme.colors.danger,
                errorLabelColor = AppTheme.colors.danger,
            ),
        )
        if (error != null) {
            Text(error, color = AppTheme.colors.danger, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondary: Boolean = false,
    compact: Boolean = false,
    gradient: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(if (compact) Modifier else Modifier.fillMaxWidth()).heightIn(min = if (compact) Sizing.touchTarget else Sizing.actionHeight)
            .then(if (gradient && enabled && !secondary) Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(Radius.medium)).actionGradient() else Modifier),
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = if (compact) Spacing.sm else Spacing.md),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) AppTheme.colors.surface else if (gradient) androidx.compose.ui.graphics.Color.Transparent else AppTheme.colors.action,
            contentColor = if (secondary) AppTheme.colors.onSurface else AppTheme.colors.onAccent,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}
