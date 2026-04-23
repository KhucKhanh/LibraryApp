package com.example.libraryapp.model

data class Chat(
    val id: String,
    val title: String,
    val lastMessage: String,
    val createdAt: Long
)