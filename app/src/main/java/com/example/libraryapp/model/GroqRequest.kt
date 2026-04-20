package com.example.libraryapp.model

data class GroqRequest(
    val model: String = "llama3-8b-8192",
    val messages: List<MessageRequest>
)