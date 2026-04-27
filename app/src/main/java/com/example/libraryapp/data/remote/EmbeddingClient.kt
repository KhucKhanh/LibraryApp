package com.example.libraryapp.ai

import com.example.libraryapp.data.remote.GeminiContent
import com.example.libraryapp.data.remote.GeminiEmbeddingClient
import com.example.libraryapp.data.remote.GeminiEmbeddingRequest
import com.example.libraryapp.data.remote.GeminiPart

object EmbeddingClient {

    private const val API_KEY = "AIzaSyCj9vk1a9SNx-Jt65UQu0SeaYLUbQgZx2I"

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