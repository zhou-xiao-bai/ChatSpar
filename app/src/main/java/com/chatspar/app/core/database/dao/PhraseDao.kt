package com.chatspar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatspar.app.core.database.entity.PhraseEntity

@Dao
interface PhraseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(phrase: PhraseEntity)

    @Query("SELECT * FROM phrases WHERE id = :id")
    fun getById(id: String): PhraseEntity?

    @Query("SELECT * FROM phrases WHERE source_review_id = :sourceReviewId ORDER BY created_at DESC")
    fun getBySourceReviewId(sourceReviewId: String): List<PhraseEntity>

    @Query("SELECT * FROM phrases WHERE source_review_id = :sourceReviewId AND content = :content LIMIT 1")
    fun getBySourceReviewIdAndContent(
        sourceReviewId: String,
        content: String,
    ): PhraseEntity?

    @Query("SELECT * FROM phrases ORDER BY created_at DESC")
    fun getAll(): List<PhraseEntity>

    @Query("DELETE FROM phrases WHERE id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM phrases")
    fun deleteAll()
}
