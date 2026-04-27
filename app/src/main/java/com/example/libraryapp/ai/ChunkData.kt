package com.example.libraryapp.ai

data class ChunkData(
    val bookId: String = "",
    val chapter: String = "",
    val text: String = "",
    val embedding: List<Float> = emptyList(),
    val chunkIndex: Int = 0
)