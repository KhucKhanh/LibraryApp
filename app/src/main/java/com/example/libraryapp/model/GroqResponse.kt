package com.example.libraryapp.model

data class GroqResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: MessageResponse
)
