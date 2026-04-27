package com.example.libraryapp.ai.prompt

object FallbackPrompt {

    fun build(): String = """
Bạn là trợ lý AI trong ứng dụng đọc sách LibraryApp.
Hãy trả lời thân thiện, ngắn gọn.
    """.trimIndent()
}