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

@Dao
interface SkipMoneyDao {
    companion object {
        private const val TAG = "SkipMoneyDao"
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
        Log.d(
            TAG,
            "recordSkippedPurchase: input label='$label', amountCents=$amountCents, createdAt=$createdAt, currentEpochDay=$currentEpochDay",
        )
        val currentSummary = getSummary()
        val shouldIncrementStreak = currentSummary?.lastStreakEpochDay != currentEpochDay
        val nextSummary = SkipMoneySummaryEntity(
            currentStreakDays = (currentSummary?.currentStreakDays ?: 0) + 1,
            totalSavedCents = (currentSummary?.totalSavedCents ?: 0L) + amountCents,
            lastStreakEpochDay = currentEpochDay,
        )

        val summaryToPersist = if (shouldIncrementStreak) {
            nextSummary
        } else {
            nextSummary.copy(
                currentStreakDays = currentSummary?.currentStreakDays ?: 0,
                lastStreakEpochDay = currentSummary?.lastStreakEpochDay ?: currentEpochDay,
            )
        }

        upsertSummary(summaryToPersist)
        Log.d(
            TAG,
            "recordSkippedPurchase: summaryBefore=${currentSummary?.totalSavedCents ?: 0L}, summaryAfter=${summaryToPersist.totalSavedCents}, shouldIncrementStreak=$shouldIncrementStreak",
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
        val existingPurchase = getSkippedPurchaseById(id) ?: return false
        val currentSummary = getSummary()
        val updatedTotalSaved = ((currentSummary?.totalSavedCents ?: 0L) - existingPurchase.amountCents + amountCents)
            .coerceAtLeast(0L)
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
