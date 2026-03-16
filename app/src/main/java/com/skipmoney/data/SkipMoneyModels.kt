package com.skipmoney.data

import java.time.LocalDate

data class SkippedPurchase(
    val id: Long,
    val label: String,
    val amountCents: Long,
    val createdAt: Long,
)

data class SkipMoneyData(
    val currentStreakDays: Int,
    val todaySavedCents: Long,
    val totalSavedCents: Long,
    val skippedPurchases: List<SkippedPurchase>,
    val savingDates: Set<LocalDate>,
    val currentMonthDailySavedCents: Map<Int, Long>,
)

data class RecordSkippedPurchaseResult(
    val shouldIncrementStreak: Boolean,
    val insertedId: Long,
    val amountCents: Long,
    val createdAt: Long,
)
