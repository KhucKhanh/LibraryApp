package com.example.libraryapp.ai.prompt

import com.example.libraryapp.ai.AIContextManager
import com.example.libraryapp.ai.FirestoreRAGRepository
import com.example.libraryapp.ai.RAGContextProvider

object BookDetailPrompt {

    suspend fun build(
        context: AIContextManager.Snapshot,
        userMessage: String
    ): String {
        val book = context.book
        val bookId = book?.id ?: ""

        val related = AIContextManager.allBooks
            .filter { it.category == book?.category && it.id != book?.id }
            .take(6)
            .joinToString("\n") { "- ${it.title} | ${it.author}" }
            .ifEmpty { "Không có sách cùng thể loại." }

        val mode = RAGContextProvider.detectMode(
            userMessage = userMessage,
            hasCurrentBook = bookId.isNotBlank()
        )



        val ragBlock = if (FirestoreRAGRepository.cacheLoaded &&
            FirestoreRAGRepository.chunksCache.isNotEmpty()) {
            val rag = RAGContextProvider.getContext(
                userMessage = userMessage,
                currentBookId = bookId,
                mode = mode
            )
            if (rag.chunks.isNotEmpty()) "\n\n${rag.contextBlock}" else ""
        } else {
            ""  // chưa có cache → bỏ qua RAG
        }

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
- Tối đa 3-4 câu mỗi lần trả lời
- Không lặp lại thông tin đã nói trước đó
- Nếu context đến từ nhiều sách, chỉ rõ nguồn

Sách cùng thể loại có trong hệ thống:
$related
$ragBlock

Hãy trả lời thân thiện, ngắn gọn, tự nhiên.
Câu hỏi: $userMessage
        """.trimIndent()
    }
}