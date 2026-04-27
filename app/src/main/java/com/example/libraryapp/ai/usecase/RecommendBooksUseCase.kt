package com.example.libraryapp.ai.usecase

import com.example.libraryapp.ai.AIContextManager

object RecommendBooksUseCase {

    fun buildPrompt(userMessage: String): String {
        val currentBook = AIContextManager.currentBook
            ?: AIContextManager.lastSelectedBook

        val relatedBooks = AIContextManager.allBooks
            .filter {
                it.category == currentBook?.category && it.id != currentBook?.id
            }
            .take(8)

        val booksText = if (relatedBooks.isNotEmpty()) {
            relatedBooks.joinToString("\n") { "- ${it.title} (${it.author})" }
        } else {
            "Không có sách cùng thể loại."
        }

        return """
Bạn là trợ lý gợi ý sách trong LibraryApp.
Nhiệm vụ: Gợi ý sách phù hợp từ danh sách có sẵn dưới đây.
Chỉ gợi ý sách trong danh sách, không tự bịa thêm.

Sách đang xem: ${currentBook?.title ?: "Chưa chọn"} (${currentBook?.category ?: ""})

Sách cùng thể loại có trong app:
$booksText

Yêu cầu: $userMessage
        """.trimIndent()
    }
}