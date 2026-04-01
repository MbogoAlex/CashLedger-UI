package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tombstone for explicitly user-removed transactionCategoryCrossRef rows.
 * When restore encounters a (transactionId, categoryId) pair that exists here,
 * it skips re-inserting it so the user's removal is preserved across restores.
 */
@Entity(tableName = "deletedCrossRefs")
data class DeletedCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val transactionId: Int,
    val categoryId: Int
)
