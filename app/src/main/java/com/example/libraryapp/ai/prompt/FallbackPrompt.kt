package com.example.libraryapp.ai.prompt

import com.example.libraryapp.ai.AIContextManager
import com.example.libraryapp.ai.BookMetadataIndexer
import com.example.libraryapp.ai.RAGContextProvider

object FallbackPrompt {

    suspend fun build(userMessage: String): String {

        val books = AIContextManager.allBooks
        BookMetadataIndexer.indexAllIfNeeded(books)

        val bookList = books.shuffled().take(20)
            .joinToString("\n") { "- ${it.title} | ${it.author} | ${it.category}" }
            .ifEmpty { "Chưa có sách nào trong hệ thống." }

        val rag = RAGContextProvider.getContext(
            userMessage = userMessage,
            mode = RAGContextProvider.RAGMode.GLOBAL
        )

        val ragBlock = if (rag.chunks.isNotEmpty()) {
            "\n\n${rag.contextBlock}"
        } else ""

        val guide = """
Hướng dẫn sử dụng app:
- Tìm sách: vào tab Tìm kiếm, gõ tên sách hoặc tác giả
- Lưu sách vào thư viện: vào trang chi tiết sách, nhấn nút "Lưu", chọn thư viện muốn lưu vào
- Tạo thư viện mới: vào tab Thư viện, nhấn nút thêm, đặt tên thư viện
- Đổi tên thư viện: vào tab Thư viện, nhấn nút 3 chấm trên thư viện, chọn "Đổi tên"
- Xoá thư viện: vào tab Thư viện, nhấn nút 3 chấm trên thư viện, chọn "Xoá"
        """.trimIndent()

        return """
Bạn là trợ lý AI trong ứng dụng đọc sách LibraryApp.
Người dùng đang dùng ứng dụng, chưa chọn sách nào cụ thể.

Bạn có thể:
- Gợi ý sách phù hợp từ danh sách bên dưới (KHÔNG bịa thêm sách ngoài danh sách)
- Trả lời câu hỏi chung về sách, tác giả, thể loại
- Hỗ trợ người dùng tìm sách theo nhu cầu, chủ đề, tác giả cụ thể
- Hướng dẫn sử dụng các tính năng trong app
- Nếu context đến từ nhiều sách, chỉ rõ nguồn

$guide

Danh sách sách hiện có:
$bookList
$ragBlock

Hãy trả lời thân thiện, ngắn gọn, tự nhiên. Tối đa 3-4 câu.
Câu hỏi: $userMessage
        """.trimIndent()
    }
}