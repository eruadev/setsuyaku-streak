package com.skipmoney.data.local

import android.util.Log
import com.skipmoney.data.RecordSkippedPurchaseResult
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

internal class TotalSavedOverflowException : IllegalStateException("totalSavedCents overflow")

internal fun checkedAddTotalSaved(
    currentTotalSaved: Long,
    amountToAdd: Long,
): Long {
    require(amountToAdd >= 0L) { "Amount to add must be non-negative." }
    if (currentTotalSaved > Long.MAX_VALUE - amountToAdd) {
        throw TotalSavedOverflowException()
    }
    return currentTotalSaved + amountToAdd
}

internal fun checkedReplaceTotalSavedAmount(
    currentTotalSaved: Long,
    previousAmount: Long,
    newAmount: Long,
): Long {
    require(previousAmount >= 0L) { "Previous amount must be non-negative." }
    require(newAmount >= 0L) { "New amount must be non-negative." }
    val adjustedTotal = (currentTotalSaved - previousAmount).coerceAtLeast(0L)
    return checkedAddTotalSaved(
        currentTotalSaved = adjustedTotal,
        amountToAdd = newAmount,
    )
}

@Dao
interface SkipMoneyDao {
    companion object {
        private const val TAG = "SkipMoneyDao"
        private const val BRANCH_SAME_DAY = "same_day"
        private const val BRANCH_INCREMENT = "increment"
        private const val BRANCH_RESET_TO_1 = "reset_to_1"
    }

    @Query("SELECT * FROM skip_money_summary WHERE id = 1")
    fun observeSummary(): Flow<SkipMoneySummaryEntity?>

    @Query("SELECT * FROM skipped_purchase ORDER BY createdAt DESC, id DESC")
    fun observeSkippedPurchases(): Flow<List<SkippedPurchaseEntity>>

    @Query("SELECT * FROM skip_money_summary WHERE id = 1")
    suspend fun getSummary(): SkipMoneySummaryEntity?

    @Query("SELECT * FROM skipped_purchase ORDER BY createdAt DESC, id DESC")
    suspend fun getSkippedPurchases(): List<SkippedPurchaseEntity>

    @Query("SELECT * FROM skipped_purchase WHERE id = :id LIMIT 1")
    suspend fun getSkippedPurchaseById(id: Long): SkippedPurchaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: SkipMoneySummaryEntity)

    @Insert
    suspend fun insertSkippedPurchase(purchase: SkippedPurchaseEntity): Long

    @Update
    suspend fun updateSkippedPurchaseEntity(purchase: SkippedPurchaseEntity)

    @Query("DELETE FROM skipped_purchase WHERE id = :id")
    suspend fun deleteSkippedPurchaseById(id: Long)

    @Query("DELETE FROM skipped_purchase")
    suspend fun deleteAllSkippedPurchases()

    @Query("DELETE FROM skip_money_summary")
    suspend fun deleteSummary()

    @Transaction
    suspend fun recordSkippedPurchase(
        label: String,
        amountCents: Long,
        createdAt: Long,
        currentEpochDay: Long,
    ): RecordSkippedPurchaseResult {
        require(amountCents >= 1L) { "Amount must be at least 1 yen before inserting." }
        Log.d(
            TAG,
            "recordSkippedPurchase: input label='$label', amountCents=$amountCents, createdAt=$createdAt, currentEpochDay=$currentEpochDay",
        )
        val currentSummary = getSummary()
        val previousStreakDays = currentSummary?.currentStreakDays ?: 0
        val lastStreakEpochDay = currentSummary?.lastStreakEpochDay
        val createdAtLocalDate = Instant.ofEpochMilli(createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val difference = lastStreakEpochDay?.let { currentEpochDay - it }
        val branchLabel = when (difference) {
            0L -> BRANCH_SAME_DAY
            1L -> BRANCH_INCREMENT
            else -> BRANCH_RESET_TO_1
        }
        val nextStreakDays = when (branchLabel) {
            BRANCH_SAME_DAY -> previousStreakDays
            BRANCH_INCREMENT -> previousStreakDays + 1
            else -> 1
        }
        val shouldIncrementStreak = branchLabel == BRANCH_INCREMENT
        val updatedTotalSaved = checkedAddTotalSaved(
            currentTotalSaved = currentSummary?.totalSavedCents ?: 0L,
            amountToAdd = amountCents,
        )
        val summaryToPersist = SkipMoneySummaryEntity(
            currentStreakDays = nextStreakDays,
            totalSavedCents = updatedTotalSaved,
            lastStreakEpochDay = currentEpochDay,
        )

        upsertSummary(summaryToPersist)
        Log.d(
            TAG,
            "saveRecord: currentEpochDay=$currentEpochDay, lastStreakEpochDay=$lastStreakEpochDay, createdAtDate=$createdAtLocalDate, difference=$difference, branch=$branchLabel, savedStreak=${summaryToPersist.currentStreakDays}, totalSavedBefore=${currentSummary?.totalSavedCents ?: 0L}, totalSavedAfter=${summaryToPersist.totalSavedCents}, shouldIncrementStreak=$shouldIncrementStreak",
        )
        val insertedId = insertSkippedPurchase(
            SkippedPurchaseEntity(
                label = label,
                amountCents = amountCents,
                createdAt = createdAt,
            ),
        )
        Log.d(
            TAG,
            "recordSkippedPurchase: persistedPurchase insertedId=$insertedId, savedAmountCents=$amountCents, savedCreatedAt=$createdAt",
        )
        return RecordSkippedPurchaseResult(
            shouldIncrementStreak = shouldIncrementStreak,
            currentStreakDays = summaryToPersist.currentStreakDays,
            streakBranch = branchLabel,
            insertedId = insertedId,
            amountCents = amountCents,
            createdAt = createdAt,
        )
    }

    @Transaction
    suspend fun updateSkippedPurchase(
        id: Long,
        label: String,
        amountCents: Long,
    ): Boolean {
        require(amountCents >= 1L) { "Amount must be at least 1 yen before updating." }
        val existingPurchase = getSkippedPurchaseById(id) ?: return false
        val currentSummary = getSummary()
        val updatedTotalSaved = checkedReplaceTotalSavedAmount(
            currentTotalSaved = currentSummary?.totalSavedCents ?: 0L,
            previousAmount = existingPurchase.amountCents,
            newAmount = amountCents,
        )
        currentSummary?.let { summary ->
            upsertSummary(summary.copy(totalSavedCents = updatedTotalSaved))
        }
        updateSkippedPurchaseEntity(
            existingPurchase.copy(
                label = label,
                amountCents = amountCents,
            ),
        )
        Log.d(
            TAG,
            "updateSkippedPurchase: id=$id, oldAmount=${existingPurchase.amountCents}, newAmount=$amountCents, updatedTotalSaved=$updatedTotalSaved",
        )
        return true
    }

    @Transaction
    suspend fun deleteSkippedPurchase(
        id: Long,
    ): Boolean {
        val existingPurchase = getSkippedPurchaseById(id) ?: return false
        val currentSummary = getSummary()
        val updatedTotalSaved = ((currentSummary?.totalSavedCents ?: 0L) - existingPurchase.amountCents)
            .coerceAtLeast(0L)
        currentSummary?.let { summary ->
            upsertSummary(summary.copy(totalSavedCents = updatedTotalSaved))
        }
        deleteSkippedPurchaseById(id)
        Log.d(
            TAG,
            "deleteSkippedPurchase: id=$id, deletedAmount=${existingPurchase.amountCents}, updatedTotalSaved=$updatedTotalSaved",
        )
        return true
    }

    @Transaction
    suspend fun resetAllData() {
        deleteAllSkippedPurchases()
        deleteSummary()
    }
}
