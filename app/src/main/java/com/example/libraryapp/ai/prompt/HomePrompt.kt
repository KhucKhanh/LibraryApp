package com.example.libraryapp.ai.prompt

import com.example.libraryapp.ai.AIContextManager

object HomePrompt {

    fun build(): String {
        val books = AIContextManager.allBooks
            .take(10)
            .joinToString("\n") { "- ${it.title} | ${it.author} | ${it.category}" }
            .ifEmpty { "Chưa có sách nào trong hệ thống." }

        return """
Bạn là trợ lý AI trong ứng dụng đọc sách LibraryApp.
Người dùng đang ở màn trang chủ, chưa chọn sách nào.

Bạn có thể:
- Gợi ý sách phù hợp từ danh sách bên dưới (KHÔNG bịa thêm sách ngoài danh sách)
- Trả lời câu hỏi chung về sách, tác giả, thể loại
- Hỗ trợ người dùng tìm sách theo nhu cầu

Danh sách sách hiện có:
$books

Hãy trả lời thân thiện, ngắn gọn, tự nhiên.
        """.trimIndent()
    }
}