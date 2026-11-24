package com.example.floworbit.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.floworbit.data.api.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repo: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private val modelName = "gpt-4o-mini" // or the OpenRouter model you have access to

    init {
        // Room is the single source of truth for persisted messages; this replaces the whole
        // list on every change instead of appending locally, so the UI never double-shows a
        // message that was also optimistically added by sendMessage().
        viewModelScope.launch {
            repo.observeHistory().collect { entities ->
                _uiState.update { state ->
                    state.copy(
                        messages = entities.map { entity ->
                            ChatMessage(
                                text = entity.content,
                                sender = if (entity.role == "user") Sender.USER else Sender.MODEL
                            )
                        }
                    )
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        _uiState.update { it.copy(isProcessing = true, error = null) }

        viewModelScope.launch {
            repo.saveMessage(role = "user", content = text)

            try {
                val response = repo.sendMessage(modelName, text)
                val aiText = response.choices?.firstOrNull()?.message?.content
                    ?: "No response from server."
                repo.saveMessage(role = "assistant", content = aiText)
                _uiState.update { it.copy(isProcessing = false) }
            } catch (e: Exception) {
                repo.saveMessage(role = "assistant", content = "Error: ${e.message}")
                _uiState.update { it.copy(isProcessing = false, error = e.localizedMessage) }
            }
        }
    }
}
