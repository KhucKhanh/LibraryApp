package com.example.libraryapp.ai.usecase

import com.example.libraryapp.ai.AIContextManager

object SummarizeBookUseCase {

    fun buildPrompt(userMessage: String): String {
        val book = AIContextManager.currentBook
            ?: AIContextManager.lastSelectedBook

        return """
Bạn là trợ lý đọc sách trong LibraryApp.
Nhiệm vụ: Tóm tắt cuốn sách dưới đây một cách ngắn gọn, dễ hiểu.

Thông tin sách:
- Tên: ${book?.title ?: "Không rõ"}
- Tác giả: ${book?.author ?: "Không rõ"}
- Thể loại: ${book?.category ?: "Không rõ"}
- Mô tả: ${book?.description ?: "Không có"}

Yêu cầu của user: $userMessage
        """.trimIndent()
    }
}