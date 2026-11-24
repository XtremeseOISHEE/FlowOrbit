package com.example.floworbit.data.api

import android.content.Context
import com.example.floworbit.data.chat.ChatDatabase
import com.example.floworbit.data.chat.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    context: Context,
    private val api: ApiService = RetrofitClient.apiService
) {

    private val dao = ChatDatabase.getInstance(context).chatMessageDao()

    fun observeHistory(): Flow<List<ChatMessageEntity>> = dao.getAll()

    suspend fun saveMessage(role: String, content: String) {
        dao.insert(ChatMessageEntity(role = role, content = content))
    }

    // Sends a message and gets response from OpenRouter
    suspend fun sendMessage(model: String = "gpt-4o-mini", prompt: String): ChatResponse {
        val messages = listOf(Message(role = "user", content = prompt))
        val request = ChatRequest(model = model, messages = messages)
        return api.getChatCompletion(request)
    }
}
