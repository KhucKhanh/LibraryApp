package com.example.libraryapp.ai.prompt

import com.example.libraryapp.ai.AIContextManager
import com.example.libraryapp.ai.BookMetadataIndexer
import com.example.libraryapp.ai.FirestoreRAGRepository
import com.example.libraryapp.ai.RAGContextProvider

object HomePrompt {

    suspend fun build(userMessage: String): String {

        val books = AIContextManager.allBooks

        BookMetadataIndexer.indexAllIfNeeded(books)

        val totalBooks = books.size
        val bookList = books.shuffled().take(20)
            .joinToString("\n") { "- ${it.title} | ${it.author} | ${it.category}" }
            .ifEmpty { "Chưa có sách nào trong hệ thống." }

        android.util.Log.d("CHAT_DEBUG", "cacheLoaded=${FirestoreRAGRepository.cacheLoaded} | cacheSize=${FirestoreRAGRepository.chunksCache.size}")


        val ragBlock = if (FirestoreRAGRepository.cacheLoaded &&
            FirestoreRAGRepository.chunksCache.isNotEmpty()) {
            val rag = RAGContextProvider.getContext(
                userMessage = userMessage,
                mode = RAGContextProvider.RAGMode.GLOBAL
            )
            if (rag.chunks.isNotEmpty()) "\n\n${rag.contextBlock}" else ""
        } else {
            ""  // chưa index xong → bỏ qua RAG, trả lời bình thường
        }

        val guide = """
Hướng dẫn sử dụng app:
- Tìm sách: vào tab Tìm kiếm, gõ tên sách hoặc tác giả
- Lưu sách vào thư viện: vào trang chi tiết sách, nhấn nút "Lưu", chọn thư viện muốn lưu vào
- Tạo thư viện mới: vào tab Thư viện, nhấn nút thêm, đặt tên thư viện
- Đổi tên thư viện: vào tab Thư viện, nhấn nút 3 chấm trên thư viện, chọn "Đổi tên"
- Xoá thư viện: vào tab Thư viện, nhấn nút 3 chấm trên thư viện, chọn "Xoá"
- Nếu người dùng hỏi đề xuất sách thì hướng dẫn người dùng vào phần tìm sách
        """.trimIndent()

        return """
Bạn là trợ lý AI trong ứng dụng đọc sách LibraryApp.
Người dùng đang ở màn trang chủ, chưa chọn sách nào.

Bạn có thể:
- Gợi ý sách phù hợp từ danh sách bên dưới (KHÔNG bịa thêm sách ngoài danh sách)
- Trả lời câu hỏi chung về sách, tác giả, thể loại
- Hỗ trợ người dùng tìm sách theo nhu cầu, tâm trạng, chủ đề
- Hướng dẫn sử dụng các tính năng trong app

Khi người dùng hỏi chung chung như "đọc gì hôm nay", hãy CHỦ ĐỘNG gợi ý 1-2 cuốn sách cụ thể từ danh sách, không hỏi ngược lại.

$guide

Danh sách sách hiện có (hiển thị 20/${totalBooks} cuốn, hệ thống có tổng cộng $totalBooks cuốn):
$bookList
$ragBlock

Hãy trả lời thân thiện, ngắn gọn, tự nhiên. Tối đa 3-4 câu.
Câu hỏi: $userMessage
        """.trimIndent()
    }
}