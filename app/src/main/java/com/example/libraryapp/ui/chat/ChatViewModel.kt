package com.example.libraryapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.libraryapp.data.ChatRepository
import com.example.libraryapp.data.remote.FirebaseChatRepository
import com.example.libraryapp.data.remote.RetrofitClient
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
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val reply = repo.sendMessage(
                    userId = userId,
                    chatId = chatId,
                    messages = messages,
                    userText = userText
                )

                onResult(reply)
            } catch (e: Exception) {
                onResult("Lỗi: ${e.message}")
            }
        }
    }
}