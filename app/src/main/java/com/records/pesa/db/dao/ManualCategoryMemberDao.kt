package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.records.pesa.db.models.ManualCategoryMember
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ManualCategoryMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: ManualCategoryMember): Long

    @Query("SELECT * FROM manual_category_member WHERE categoryId = :categoryId AND deletedAt IS NULL ORDER BY name ASC")
    fun getForCategory(categoryId: Int): Flow<List<ManualCategoryMember>>

    @Query("SELECT * FROM manual_category_member WHERE deletedAt IS NULL")
    suspend fun getAllOnce(): List<ManualCategoryMember>

    @Query("UPDATE manual_category_member SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun deleteById(id: Int, deletedAt: LocalDateTime)

    @Query("UPDATE manual_category_member SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Int, newName: String)
}
