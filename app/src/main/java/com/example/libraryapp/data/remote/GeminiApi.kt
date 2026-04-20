//package com.example.libraryapp.data.remote
//
//import com.example.libraryapp.data.ChatRepository
//import com.example.libraryapp.model.GeminiRequest
//import com.example.libraryapp.model.GeminiResponse
//import retrofit2.http.Body
//import retrofit2.http.POST
//import retrofit2.http.Query
//
//interface GeminiApi {
//
//    @POST("v1beta/models/gemini-2.0-flash:generateContent")
//    suspend fun chat(
//        @Query("key") apiKey: String,
//        @Body request: GeminiRequest
//    ): GeminiResponse
//}