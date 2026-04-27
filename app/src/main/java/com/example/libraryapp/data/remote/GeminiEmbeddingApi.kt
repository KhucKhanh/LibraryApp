package com.example.libraryapp.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiEmbeddingApi {

    @POST("models/{model}:embedContent")
    suspend fun embed(
        @Path("model") model: String = "gemini-embedding-001",
        @Query("key") apiKey: String,
        @Body request: GeminiEmbeddingRequest
    ): GeminiEmbeddingResponse
}

data class GeminiEmbeddingRequest(
    val model: String = "models/gemini-embedding-001",
    val content: GeminiContent
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiEmbeddingResponse(
    val embedding: GeminiEmbeddingValues
)

data class GeminiEmbeddingValues(
    val values: List<Float>
)