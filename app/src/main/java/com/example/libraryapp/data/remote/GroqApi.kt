package com.example.libraryapp.data.remote

import com.example.libraryapp.model.GroqRequest
import com.example.libraryapp.model.GroqResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApi {

    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: GroqRequest
    ): GroqResponse
}