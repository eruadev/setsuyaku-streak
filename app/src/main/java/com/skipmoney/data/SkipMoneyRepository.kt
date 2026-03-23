package com.skipmoney.data

import android.util.Log
import com.skipmoney.data.local.SkipMoneyDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class SkipMoneyRepository(
    private val skipMoneyDao: SkipMoneyDao,
) {
    private companion object {
        const val TAG = "SkipMoneyRepository"
    }

    val data: Flow<SkipMoneyData> =
        skipMoneyDao.observeSummary()
            .combine(skipMoneyDao.observeSkippedPurchases(), ::buildSkipMoneyData)

    suspend fun recordSkippedPurchase(
        title: String,
        amountCents: Long,
    ): RecordSkippedPurchaseResult {
        require(amountCents >= 1L) { "Amount must be at least 1 yen before persistence." }
        return skipMoneyDao.recordSkippedPurchase(
            label = title,
            amountCents = amountCents,
            createdAt = System.currentTimeMillis(),
            currentEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay(),
        ).also { result ->
            Log.d(
                TAG,
                "recordSkippedPurchase: incomingAmountCents=$amountCents, insertedId=${result.insertedId}, storedAmountCents=${result.amountCents}, createdAt=${result.createdAt}",
            )
        }
    }

    suspend fun resetAllData() {
        skipMoneyDao.resetAllData()
    }

    suspend fun updateSkippedPurchase(
        id: Long,
        title: String,
        amountCents: Long,
    ): Boolean {
        require(amountCents >= 1L) { "Amount must be at least 1 yen before persistence." }
        return skipMoneyDao.updateSkippedPurchase(
            id = id,
            label = title,
            amountCents = amountCents,
        ).also { updated ->
            Log.d(
                TAG,
                "updateSkippedPurchase: id=$id, amountCents=$amountCents, updated=$updated",
            )
        }
    }

    suspend fun deleteSkippedPurchase(
        id: Long,
    ): Boolean =
        skipMoneyDao.deleteSkippedPurchase(id).also { deleted ->
            Log.d(TAG, "deleteSkippedPurchase: id=$id, deleted=$deleted")
        }

    suspend fun getCurrentData(): SkipMoneyData =
        buildSkipMoneyData(
            summary = skipMoneyDao.getSummary(),
            purchases = skipMoneyDao.getSkippedPurchases(),
        )

    private fun buildSkipMoneyData(
        summary: com.skipmoney.data.local.SkipMoneySummaryEntity?,
        purchases: List<com.skipmoney.data.local.SkippedPurchaseEntity>,
    ): SkipMoneyData {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val currentMonth = YearMonth.now(zoneId)
        val purchasesByDate = purchases.groupBy { purchase ->
            Instant.ofEpochMilli(purchase.createdAt)
                .atZone(zoneId)
                .toLocalDate()
        }
        val todaySavedCents = purchasesByDate[today]?.sumOf { it.amountCents } ?: 0L
        val currentMonthEntries = purchasesByDate.entries
            .filter { (date, _) -> YearMonth.from(date) == currentMonth }
            .associate { (date, entries) ->
                date.dayOfMonth to entries.sumOf { it.amountCents }
            }

        Log.d(
            TAG,
            "buildSkipMoneyData: zone=$zoneId, currentMonth=$currentMonth, currentMonthRecordCount=${purchases.count { purchase -> YearMonth.from(Instant.ofEpochMilli(purchase.createdAt).atZone(zoneId).toLocalDate()) == currentMonth }}",
        )
        purchases.forEach { purchase ->
            val zonedDateTime = Instant.ofEpochMilli(purchase.createdAt).atZone(zoneId)
            Log.d(
                TAG,
                "buildSkipMoneyData: record id=${purchase.id}, amountCents=${purchase.amountCents}, createdAt=${purchase.createdAt}, zonedDateTime=$zonedDateTime, yearMonth=${YearMonth.from(zonedDateTime)}",
            )
        }
        Log.d(TAG, "buildSkipMoneyData: today=$today, todaySavedCents=$todaySavedCents")
        Log.d(TAG, "buildSkipMoneyData: currentMonthDailySavedCents=$currentMonthEntries")

        return SkipMoneyData(
            currentStreakDays = summary?.currentStreakDays ?: 0,
            todaySavedCents = todaySavedCents,
            totalSavedCents = summary?.totalSavedCents ?: 0L,
            skippedPurchases = purchases.map { purchase ->
                SkippedPurchase(
                    id = purchase.id,
                    label = purchase.label,
                    amountCents = purchase.amountCents,
                    createdAt = purchase.createdAt,
                )
            },
            savingDates = purchases.mapTo(linkedSetOf()) { purchase ->
                Instant.ofEpochMilli(purchase.createdAt)
                    .atZone(zoneId)
                    .toLocalDate()
            },
            currentMonthDailySavedCents = currentMonthEntries,
        )
    }
}
