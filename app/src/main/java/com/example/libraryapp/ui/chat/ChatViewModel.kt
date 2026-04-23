package com.example.libraryapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.libraryapp.data.ChatRepository
import com.example.libraryapp.data.remote.FirebaseChatRepository
import com.example.libraryapp.data.remote.RetrofitClient
import com.example.libraryapp.model.Chat
import com.example.libraryapp.model.Message
import com.example.libraryapp.model.MessageRequest
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repo = ChatRepository(
        RetrofitClient.api,
        FirebaseChatRepository()
    )

    fun sendMessage(
        userId: String,
        chatId: String,
        userText: String,
        messages: List<MessageRequest>,
        isFirstMessage: Boolean,  // ✅
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val reply = repo.sendMessage(
                    userId = userId,
                    chatId = chatId,
                    messages = messages,
                    userText = userText,
                    isFirstMessage = isFirstMessage  // ✅
                )
                onResult(reply)
            } catch (e: Exception) {
                onResult("Lỗi: ${e.message}")
            }
        }
    }

    fun loadChatHistory(userId: String, chatId: String, onLoaded: (List<Message>) -> Unit) {
        repo.loadChatHistory(userId, chatId, onLoaded)
    }

    fun loadChatList(userId: String, onLoaded: (List<Chat>) -> Unit) {
        repo.loadChatList(userId, onLoaded)
    }
}