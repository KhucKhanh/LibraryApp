package com.example.libraryapp.ai.usecase

import com.example.libraryapp.ai.AIContextManager

object GeneralChatUseCase {

    fun buildPrompt(userMessage: String): String {
        val book = AIContextManager.currentBook
            ?: AIContextManager.lastSelectedBook

        return """
Bạn là trợ lý AI trong ứng dụng đọc sách LibraryApp.
Hãy trả lời thân thiện, ngắn gọn.

${if (book != null) "User đang xem sách: ${book.title}" else ""}

Câu hỏi: $userMessage
        """.trimIndent()
    }
}