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

            if (isFirstMessage) {
                firebaseRepo.createChatMetadata(userId, chatId, userText)
            } else {
                firebaseRepo.updateLastMessage(userId, chatId, userText)
            }

            val userTime = System.currentTimeMillis()
            firebaseRepo.saveMessage(userId, chatId, Message(userText, true), userTime)
            firebaseRepo.saveMessage(userId, chatId, Message(aiText, false), userTime + 1)

            Log.d("GROQ_REQUEST", request.toString())
            aiText

        } catch (e: Exception) {
            val errorText = "Error: ${e.message}"
            val userTime = System.currentTimeMillis()
            firebaseRepo.saveMessage(userId, chatId, Message(userText, true), userTime)
            firebaseRepo.saveMessage(userId, chatId, Message(errorText, false), userTime + 1)
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