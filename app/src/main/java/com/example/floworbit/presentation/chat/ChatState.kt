package com.example.floworbit.presentation.chat

/**
 * Represents a single message in the chat, including who sent it.
 */
data class ChatMessage(
    val text: String,
    val sender: Sender,
    val isProcessing: Boolean = false // To show a thinking indicator
)

/**
 * An enum to easily distinguish between the user and the AI model.
 */
enum class Sender {
    USER, MODEL
}

/**
 * Represents the complete state of the chat screen at any given moment.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isProcessing: Boolean = false,
    val error: String? = null
)
