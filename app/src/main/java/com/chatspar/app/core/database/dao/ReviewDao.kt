package com.chatspar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatspar.app.core.database.entity.ReviewEntity

@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(review: ReviewEntity)

    @Query("SELECT * FROM reviews WHERE id = :id")
    fun getById(id: String): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE session_id = :sessionId")
    fun getBySessionId(sessionId: String): ReviewEntity?

    @Query("SELECT * FROM reviews ORDER BY created_at DESC")
    fun getAll(): List<ReviewEntity>

    @Query("DELETE FROM reviews WHERE id = :id")
    fun deleteById(id: String)
}
