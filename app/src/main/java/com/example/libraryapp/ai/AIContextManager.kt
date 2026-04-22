package com.example.libraryapp.ai

import com.example.libraryapp.model.Book

object AIContextManager {

    var currentScreen: String = "Home"
    var currentBook: Book? = null
    var currentChapter: String? = null
    var currentChapterContent: String? = null
    var allBooks: List<Book> = emptyList()
    var lastSelectedBook: Book? = null

    fun buildPrompt(userMessage: String): String {

        val book = currentBook

        // 🔥 LỌC SÁCH LIÊN QUAN (QUAN TRỌNG NHẤT)
        val relevantBooks = allBooks
            .filter {
                it.category == lastSelectedBook?.category
                        && it.id != lastSelectedBook?.id
            }
            .take(10)

        val relatedBooksText = if (relevantBooks.isNotEmpty()) {
            relevantBooks.joinToString("\n") {
                "- ${it.title} (${it.category})"
            }
        } else {
            "Không có"
        }

        return """
Bạn là trợ lý AI trong ứng dụng đọc sách LibraryApp.

=== NGỮ CẢNH HIỆN TẠI ===
Màn hình: $currentScreen

=== SÁCH ĐANG XEM ===
- Tên: ${book?.title ?: lastSelectedBook?.title ?: "Không có"}
- Tác giả: ${book?.author ?: lastSelectedBook?.author ?: "Không có"}
- Mô tả: ${book?.description ?: lastSelectedBook?.description ?: "Không có"}
- Thể loại: ${book?.category ?: lastSelectedBook?.category ?: "Không có"}

=== SÁCH LIÊN QUAN ===
$relatedBooksText

=== CHƯƠNG ===
- ${currentChapter ?: "Không có"}

=== NỘI DUNG CHƯƠNG ===
${currentChapterContent?.take(1000) ?: "Không có"}

=== NHIỆM VỤ ===
- Nếu người dùng hỏi "cuốn này" → hiểu là sách đang xem
- Nếu hỏi sách tương tự → dùng danh sách "SÁCH LIÊN QUAN"
- Gợi ý sách cụ thể từ danh sách trên
- Khuyến khích người dùng tìm sách trong mục Search
- Trả lời ngắn gọn, đúng trọng tâm

Câu hỏi:
$userMessage
        """.trimIndent()
    }
}