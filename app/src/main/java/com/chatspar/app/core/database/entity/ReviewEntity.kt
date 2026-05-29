package com.chatspar.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = PracticeSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["session_id"], unique = true),
        Index(value = ["scenario_id"]),
    ],
)
data class ReviewEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "scenario_id")
    val scenarioId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "overall_summary")
    val overallSummary: String,
    @ColumnInfo(name = "scores_json")
    val scoresJson: String,
    @ColumnInfo(name = "problems_json")
    val problemsJson: String,
    @ColumnInfo(name = "key_moments_json")
    val keyMomentsJson: String,
    @ColumnInfo(name = "suggested_expressions_json")
    val suggestedExpressionsJson: String,
    @ColumnInfo(name = "next_suggestion")
    val nextSuggestion: String,
    @ColumnInfo(name = "raw_response")
    val rawResponse: String,
)
