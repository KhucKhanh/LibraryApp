package com.example.libraryapp.data

import android.util.Log
import com.example.libraryapp.BuildConfig
import com.example.libraryapp.data.remote.GroqApi
import com.example.libraryapp.model.*

class ChatRepository(
    private val api: GroqApi
) {

    suspend fun sendMessage(messages: List<MessageRequest>): String {
        return try {

            val request = GroqRequest(
                model = "llama-3.1-8b-instant",
                messages = messages
            )

            val response = api.chat(
                auth = "Bearer ${BuildConfig.GROQ_API_KEY}",
                request = request
            )

            Log.d("GROQ_REQUEST", request.toString())

            response.choices.first().message.content

        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}