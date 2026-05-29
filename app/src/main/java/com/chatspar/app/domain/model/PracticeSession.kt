package com.chatspar.app.domain.model

import java.time.OffsetDateTime

data class PracticeSession(
    val id: String,
    val scenarioId: String,
    val status: SessionStatus,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val messageCount: Int,
    val reviewId: String?,
)

enum class SessionStatus(
    val value: String,
) {
    IN_PROGRESS("in_progress"),
    REVIEWING("reviewing"),
    COMPLETED("completed"),
    FAILED("failed"),
}
