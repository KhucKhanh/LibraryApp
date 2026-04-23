package com.example.libraryapp.data

import android.util.Log
import com.example.libraryapp.BuildConfig
import com.example.libraryapp.data.remote.FirebaseChatRepository
import com.example.libraryapp.data.remote.GroqApi
import com.example.libraryapp.model.*

class ChatRepository(
    private val api: GroqApi,
    private val firebaseRepo: FirebaseChatRepository
) {

    suspend fun sendMessage(
        userId: String,
        chatId: String,
        messages: List<MessageRequest>,
        userText: String,
        isFirstMessage: Boolean  // ✅ thêm param này
    ): String {
        return try {
            Log.d("AI_REQUEST", messages.toString())

            val request = GroqRequest(
                model = "llama-3.1-8b-instant",
                messages = messages
            )

            val response = api.chat(
                auth = "Bearer ${BuildConfig.GROQ_API_KEY}",
                request = request
            )

            val aiText = response.choices.first().message.content

            // ✅ Nếu là tin đầu tiên thì tạo metadata
            if (isFirstMessage) {
                firebaseRepo.createChatMetadata(userId, chatId, userText)
            } else {
                firebaseRepo.updateLastMessage(userId, chatId, userText)
            }

            firebaseRepo.saveMessage(userId, chatId, Message(userText, true))
            firebaseRepo.saveMessage(userId, chatId, Message(aiText, false))

            Log.d("GROQ_REQUEST", request.toString())
            aiText

        } catch (e: Exception) {
            val errorText = "Error: ${e.message}"
            firebaseRepo.saveMessage(userId, chatId, Message(userText, true))
            firebaseRepo.saveMessage(userId, chatId, Message(errorText, false))
            errorText
        }
    }

    fun loadChatHistory(userId: String, chatId: String, onResult: (List<Message>) -> Unit) {
        firebaseRepo.loadChatHistory(userId, chatId, onResult)
    }

    fun loadChatList(userId: String, onResult: (List<Chat>) -> Unit) {
        firebaseRepo.loadChatList(userId, onResult)
    }
}