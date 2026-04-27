package com.example.libraryapp.data.remote

import com.example.libraryapp.BuildConfig
import com.example.libraryapp.data.remote.GeminiContent
import com.example.libraryapp.data.remote.GeminiEmbeddingClient
import com.example.libraryapp.data.remote.GeminiEmbeddingRequest
import com.example.libraryapp.data.remote.GeminiPart

object EmbeddingClient {

    private const val API_KEY = BuildConfig.GEMINI_API_KEY

    suspend fun embed(text: String): List<Float>? {
        return try {
            val response = GeminiEmbeddingClient.api.embed(
                apiKey = API_KEY,
                request = GeminiEmbeddingRequest(
                    content = GeminiContent(
                        parts = listOf(GeminiPart(text = text.take(8000)))
                    )
                )
            )
            response.embedding.values
        } catch (e: Exception) {
            null // fallback về Jaccard nếu lỗi
        }
    }
}