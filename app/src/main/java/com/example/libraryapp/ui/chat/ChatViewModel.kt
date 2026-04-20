package com.example.libraryapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.libraryapp.data.ChatRepository
import com.example.libraryapp.data.remote.RetrofitClient
import com.example.libraryapp.model.MessageRequest
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repo = ChatRepository(RetrofitClient.api)

    fun sendMessage(
        messages: List<MessageRequest>,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val reply = repo.sendMessage(messages)
                onResult(reply)
            } catch (e: Exception) {
                onResult("Lỗi: ${e.message}")
            }
        }
    }
}