package com.example.libraryapp.data.remote

import android.util.Log
import com.example.libraryapp.BuildConfig

object EmbeddingClient {

    private const val API_KEY = BuildConfig.GEMINI_API_KEY

    suspend fun embed(text: String): List<Float>? {
        Log.d("RAG_DEBUG", "Calling Gemini embed, text length: ${text.length}")
        return try {
            val response = GeminiEmbeddingClient.api.embed(
                model = "gemini-embedding-001",
                apiKey = API_KEY,
                request = GeminiEmbeddingRequest(
                    content = GeminiContent(
                        parts = listOf(GeminiPart(text = text.take(3000)))
                    )
                )
            )
            Log.d("RAG_DEBUG", "Gemini embed response OK, values: ${response.embedding.values.size}")
            response.embedding.values

        } catch (e: Exception) {
            Log.e("RAG_DEBUG", "Embedding error: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
}