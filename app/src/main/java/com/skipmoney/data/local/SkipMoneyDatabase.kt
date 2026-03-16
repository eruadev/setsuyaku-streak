package com.skipmoney.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SkipMoneySummaryEntity::class, SkippedPurchaseEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class SkipMoneyDatabase : RoomDatabase() {

    abstract fun skipMoneyDao(): SkipMoneyDao

    companion object {
        @Volatile
        private var instance: SkipMoneyDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE skip_money_summary ADD COLUMN lastStreakEpochDay INTEGER",
                )
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Normalize legacy pseudo-cents into whole yen for the Japanese release build.
                db.execSQL(
                    "UPDATE skip_money_summary SET totalSavedCents = CAST(totalSavedCents / 100 AS INTEGER)",
                )
                db.execSQL(
                    "UPDATE skipped_purchase SET amountCents = CAST(amountCents / 100 AS INTEGER)",
                )
            }
        }

        fun getInstance(context: Context): SkipMoneyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SkipMoneyDatabase::class.java,
                    "skipmoney.db",
                )
                    .addMigrations(migration1To2)
                    .addMigrations(migration2To3)
                    .build()
                    .also { instance = it }
            }
    }
}
