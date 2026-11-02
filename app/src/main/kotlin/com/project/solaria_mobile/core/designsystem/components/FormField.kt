package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.project.solaria_mobile.core.designsystem.theme.actionGradient
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing

@OptIn(ExperimentalMaterial3Api::class)
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
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        // Campo preenchido sem contorno: o rótulo é o placeholder enquanto vazio e sobe para o topo do próprio campo ao digitar.
        // Só o erro ganha borda; em foco o campo não muda de aparência além do cursor e da cor do rótulo.
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.medium)
        val clear = androidx.compose.ui.graphics.Color.Transparent
        val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        val colors = TextFieldDefaults.colors(
            focusedContainerColor = AppTheme.colors.inputFill,
            unfocusedContainerColor = AppTheme.colors.inputFill,
            disabledContainerColor = AppTheme.colors.inputFill,
            errorContainerColor = AppTheme.colors.inputFill,
            focusedIndicatorColor = clear,
            unfocusedIndicatorColor = clear,
            disabledIndicatorColor = clear,
            errorIndicatorColor = clear,
            focusedLabelColor = AppTheme.colors.onSurfaceMuted,
            errorLabelColor = AppTheme.colors.danger,
        )
        val errorBorder = if (error != null) Modifier.border(1.5.dp, AppTheme.colors.danger, shape) else Modifier
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = TextFieldDefaults.MinHeight).clip(shape).then(errorBorder)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppTheme.colors.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            visualTransformation = visualTransformation,
            interactionSource = interaction,
            decorationBox = { inner ->
                TextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = inner,
                    enabled = enabled,
                    singleLine = singleLine,
                    visualTransformation = visualTransformation,
                    interactionSource = interaction,
                    isError = error != null,
                    label = { Text(label) },
                    shape = shape,
                    colors = colors,
                    // Menos espaço acima do rótulo sobe o texto pequeno; o de baixo compensa para o campo não encolher.
                    contentPadding = TextFieldDefaults.contentPaddingWithLabel(top = 4.dp, bottom = 12.dp),
                    container = { TextFieldDefaults.Container(enabled, error != null, interaction, Modifier, colors, shape) },
                )
            },
        )
        if (error != null) {
            Appear { Text(error, color = AppTheme.colors.danger, style = MaterialTheme.typography.bodySmall) }
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
) {
    AppButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = if (secondary) ButtonVariant.Secondary else ButtonVariant.Primary,
        size = if (compact) ButtonSize.Medium else ButtonSize.Large,
        enabled = enabled,
    )
}
