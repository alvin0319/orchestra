package dev.minjae.orchestra.config

data class BotConfig(
    val token: String,
    val ownerId: Long,
    val prefix: String,
    val status: String,
    val activity: String,
    val activityType: String,
    val proxiedYTM: Boolean,
    val proxiedYTMURL: String,
    val debug: Boolean
)
