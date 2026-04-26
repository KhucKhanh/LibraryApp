package com.example.libraryapp.model

data class Library(
    val id: String = "",
    val name: String = "",
    val books: List<String> = emptyList(),
    val createdAt: Long = 0L
)