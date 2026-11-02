package com.project.solaria_mobile.feature.address

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.project.solaria_mobile.feature.address.domain.ZIP_LENGTH
import com.project.solaria_mobile.feature.address.domain.formatZip

private const val DASH_AFTER = 5

/**
 * Mostra "74000-140" enquanto o campo guarda só "74000140". A máscara é visual: o texto digitado não muda de tamanho,
 * então o cursor nunca pula, mesmo com digitação rápida.
 */
object ZipVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(ZIP_LENGTH)
        val formatted = formatZip(digits)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                (if (offset <= DASH_AFTER) offset else offset + 1).coerceAtMost(formatted.length)

            override fun transformedToOriginal(offset: Int): Int =
                (if (offset <= DASH_AFTER) offset else offset - 1).coerceIn(0, digits.length)
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
