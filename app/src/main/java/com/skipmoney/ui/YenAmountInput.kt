package com.skipmoney.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

internal fun parseWholeYenOrNull(rawInput: String): Long? {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.any { !it.isDigit() }) return null
    if (trimmed.length > 1 && trimmed.startsWith('0')) return null

    val amount = trimmed.toLongOrNull() ?: return null
    if (amount < 1L) return null

    return amount
}

internal fun formatWholeYenForDisplay(amount: Long): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(amount)

internal object WholeYenGroupingVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formatted = raw.toLongOrNull()?.let(::formatWholeYenForDisplay) ?: raw
        val originalToTransformed = IntArray(raw.length + 1)
        val transformedToOriginal = IntArray(formatted.length + 1)

        var originalIndex = 0
        formatted.forEachIndexed { transformedIndex, char ->
            if (char.isDigit() && originalIndex < raw.length) {
                originalToTransformed[originalIndex] = transformedIndex
                transformedToOriginal[transformedIndex] = originalIndex
                originalIndex++
            } else {
                transformedToOriginal[transformedIndex] = originalIndex
            }
        }
        originalToTransformed[raw.length] = formatted.length
        transformedToOriginal[formatted.length] = raw.length

        return TransformedText(
            text = AnnotatedString(formatted),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    originalToTransformed[offset.coerceIn(0, raw.length)]

                override fun transformedToOriginal(offset: Int): Int =
                    transformedToOriginal[offset.coerceIn(0, formatted.length)]
            },
        )
    }
}
