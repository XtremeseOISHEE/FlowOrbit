package com.example.floworbit.data.api

// Represents each message in the chat conversation
data class Message(
    val role: String,   // "user", "assistant", or "system"
    val content: String
)

// Request body for OpenRouter API
data class ChatRequest(
    val model: String = "mistral-7b",  // default GPT-4o-mini
    val messages: List<Message>
)
