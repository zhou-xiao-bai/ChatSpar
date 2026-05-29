package com.chatspar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatspar.app.core.database.entity.MessageEntity

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE id = :id")
    fun getById(id: String): MessageEntity?

    @Query(
        "SELECT * FROM messages " +
            "WHERE session_id = :sessionId " +
            "ORDER BY round_index ASC, created_at ASC",
    )
    fun getBySessionId(sessionId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE session_id = :sessionId")
    fun deleteBySessionId(sessionId: String)
}
