package com.example.libraryapp.model

data class MessageEntity(
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)