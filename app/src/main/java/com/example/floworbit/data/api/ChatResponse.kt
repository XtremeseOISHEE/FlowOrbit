package com.example.floworbit.data.api

// Each choice returned by the model
data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

// Main response object
data class ChatResponse(
    val id: String,
    val objectType: String,   // e.g., "chat.completion"
    val created: Long,
    val model: String,
    val choices: List<Choice>
)
