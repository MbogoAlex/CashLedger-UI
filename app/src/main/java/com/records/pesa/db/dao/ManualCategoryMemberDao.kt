package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.records.pesa.db.models.ManualCategoryMember
import kotlinx.coroutines.flow.Flow

@Dao
interface ManualCategoryMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: ManualCategoryMember): Long

    @Query("SELECT * FROM manual_category_member WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getForCategory(categoryId: Int): Flow<List<ManualCategoryMember>>

    @Query("SELECT * FROM manual_category_member")
    suspend fun getAllOnce(): List<ManualCategoryMember>

    @Query("DELETE FROM manual_category_member WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE manual_category_member SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Int, newName: String)
}
