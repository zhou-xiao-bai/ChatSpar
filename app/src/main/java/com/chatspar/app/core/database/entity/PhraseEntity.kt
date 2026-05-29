package com.chatspar.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "phrases",
    indices = [
        Index(value = ["source_scenario_id"]),
        Index(value = ["source_review_id"]),
    ],
)
data class PhraseEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    @ColumnInfo(name = "tags_json")
    val tagsJson: String,
    @ColumnInfo(name = "source_scenario_id")
    val sourceScenarioId: String,
    @ColumnInfo(name = "source_review_id")
    val sourceReviewId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
