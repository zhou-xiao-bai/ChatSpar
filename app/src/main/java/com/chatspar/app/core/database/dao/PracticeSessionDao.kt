package com.chatspar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chatspar.app.core.database.entity.PracticeSessionEntity

@Dao
interface PracticeSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(session: PracticeSessionEntity)

    @Update
    fun update(session: PracticeSessionEntity)

    @Query("SELECT * FROM practice_sessions WHERE id = :id")
    fun getById(id: String): PracticeSessionEntity?

    @Query("SELECT * FROM practice_sessions ORDER BY started_at DESC")
    fun getAll(): List<PracticeSessionEntity>

    @Query("UPDATE practice_sessions SET message_count = :messageCount WHERE id = :id")
    fun updateMessageCount(
        id: String,
        messageCount: Int,
    )

    @Query("DELETE FROM practice_sessions WHERE id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM practice_sessions")
    fun deleteAll()
}
