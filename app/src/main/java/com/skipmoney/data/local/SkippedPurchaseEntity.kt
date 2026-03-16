package com.skipmoney.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skipped_purchase")
data class SkippedPurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val amountCents: Long,
    val createdAt: Long,
)
