package com.chatspar.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "practice_sessions",
    indices = [
        Index(value = ["scenario_id"]),
        Index(value = ["status"]),
    ],
)
data class PracticeSessionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "scenario_id")
    val scenarioId: String,
    val status: String,
    @ColumnInfo(name = "started_at")
    val startedAt: String,
    @ColumnInfo(name = "ended_at")
    val endedAt: String?,
    @ColumnInfo(name = "message_count")
    val messageCount: Int,
    @ColumnInfo(name = "review_id")
    val reviewId: String?,
)
