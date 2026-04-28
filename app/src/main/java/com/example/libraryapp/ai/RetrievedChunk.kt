package com.example.libraryapp.ai

data class RetrievedChunk(
    val text: String,
    val bookId: String,
    val chapter: String,
    val score: Float
)