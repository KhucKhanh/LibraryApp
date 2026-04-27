package com.example.libraryapp.ai.prompt

import com.example.libraryapp.ai.AIContextManager

object BookDetailPrompt {

    fun build(context: AIContextManager.Snapshot): String {
        val book = context.book
        val related = AIContextManager.allBooks
            .filter { it.category == book?.category && it.id != book?.id }
            .take(6)
            .joinToString("\n") { "- ${it.title} | ${it.author}" }
            .ifEmpty { "Không có sách cùng thể loại." }

        return """
Bạn là trợ lý AI trong ứng dụng đọc sách LibraryApp.
Người dùng đang xem chi tiết cuốn sách sau:

Tên: ${book?.title ?: "Không rõ"}
Tác giả: ${book?.author ?: "Không rõ"}
Thể loại: ${book?.category ?: "Không rõ"}
Mô tả: ${book?.description ?: "Không có"}

Bạn có thể:
- Tóm tắt, giới thiệu cuốn sách này
- Trả lời câu hỏi về nội dung, tác giả, thể loại
- Gợi ý sách tương tự từ danh sách bên dưới (KHÔNG bịa thêm)

Sách cùng thể loại có trong hệ thống:
$related

Hãy trả lời thân thiện, ngắn gọn, tự nhiên.
        """.trimIndent()
    }
}