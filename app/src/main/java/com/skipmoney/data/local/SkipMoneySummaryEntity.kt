package com.skipmoney.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skip_money_summary")
data class SkipMoneySummaryEntity(
    @PrimaryKey
    val id: Int = SUMMARY_ID,
    val currentStreakDays: Int,
    val totalSavedCents: Long,
    val lastStreakEpochDay: Long? = null,
) {
    companion object {
        const val SUMMARY_ID = 1
    }
}
