package com.skipmoney.ui

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class MoneyFormatter(
    private val locale: Locale,
    private val currency: Currency,
) {
    private val minorUnitScale = currency.defaultFractionDigits.coerceAtLeast(0)

    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = this@MoneyFormatter.currency
        maximumFractionDigits = minorUnitScale
        minimumFractionDigits = minorUnitScale
    }

    fun formatMinorUnits(amountMinor: Long): String =
        formatter.format(BigDecimal.valueOf(amountMinor, minorUnitScale))

    fun formatWholeUnits(amountWhole: Long): String =
        formatter.format(BigDecimal.valueOf(amountWhole))

    companion object {
        fun forJapaneseYen(): MoneyFormatter =
            MoneyFormatter(
                locale = Locale.JAPAN,
                currency = Currency.getInstance("JPY"),
            )
    }
}
