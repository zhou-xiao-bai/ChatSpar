package com.chatspar.app.domain.model

import java.time.OffsetDateTime

data class Phrase(
    val id: String,
    val content: String,
    val tags: List<String>,
    val sourceScenarioId: String,
    val sourceReviewId: String,
    val createdAt: OffsetDateTime,
)
