package com.example.libraryapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.libraryapp.ai.AIContextManager
import com.example.libraryapp.ai.ScreenPromptBuilder
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

    fun buildAndSend(
        contextSnapshot: AIContextManager.Snapshot,
        text: String,
        userId: String,
        chatId: String,
        isFirstMessage: Boolean,
        history: List<Message>,
        onReply: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val systemPrompt = ScreenPromptBuilder.build(contextSnapshot, text)

                val requestMessages = mutableListOf<MessageRequest>()
                requestMessages.add(MessageRequest(role = "system", content = systemPrompt))

                history.dropLast(0).takeLast(10).forEach { msg ->
                    requestMessages.add(
                        MessageRequest(
                            role = if (msg.isUser) "user" else "assistant",
                            content = msg.text
                        )
                    )
                }

                requestMessages.add(MessageRequest(role = "user", content = text))

                val reply = repo.sendMessage(
                    userId = userId,
                    chatId = chatId,
                    messages = requestMessages,
                    userText = text,
                    isFirstMessage = isFirstMessage
                )
                onReply(reply)
            } catch (e: Exception) {
                onReply("Lỗi: ${e.message}")
            }
        }
    }

}