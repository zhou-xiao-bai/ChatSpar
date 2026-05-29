package com.chatspar.app.domain.model

import java.time.OffsetDateTime

data class AppSettings(
    val apiBaseUrl: String,
    val apiKey: String,
    val modelName: String,
    val hasCompletedOnboarding: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
