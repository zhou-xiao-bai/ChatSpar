package com.chatspar.app.domain.model

import java.time.OffsetDateTime

data class PracticeMessage(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val createdAt: OffsetDateTime,
    val roundIndex: Int,
)

enum class MessageRole(
    val value: String,
) {
    SYSTEM("system"),
    ASSISTANT("assistant"),
    USER("user"),
}
