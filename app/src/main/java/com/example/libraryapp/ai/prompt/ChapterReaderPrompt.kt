package com.example.libraryapp.ai.prompt

import android.util.Log
import com.example.libraryapp.ai.AIContextManager
import com.example.libraryapp.ai.SimpleRAGEngine

object ChapterReaderPrompt {

    suspend fun build(
        context: AIContextManager.Snapshot,
        userMessage: String
    ): String {

        val book = context.book
        val chapterName = context.chapter ?: "Không rõ"
        val content = context.chapterContent
        val bookId = book?.id ?: ""

        Log.d("RAG_DEBUG", "========== ChapterReaderPrompt ==========")
        Log.d("RAG_DEBUG", "BookId: $bookId")
        Log.d("RAG_DEBUG", "Chapter: $chapterName")
        Log.d("RAG_DEBUG", "User question: $userMessage")

        // 🔥 Check content
        if (content.isNullOrBlank()) {
            Log.e("RAG_DEBUG", "❌ Content is NULL or EMPTY → RAG WILL NOT RUN")
        } else {
            Log.d("RAG_DEBUG", "✅ Content length: ${content.length}")
            Log.d("RAG_DEBUG", "Content preview: ${content.take(100)}")
        }

        // 🔥 RAG retrieval
        val retrievedContext = if (!content.isNullOrBlank()) {

            Log.d("RAG_DEBUG", "➡️ Calling indexChapter...")
            SimpleRAGEngine.indexChapter(bookId, chapterName, content)

            Log.d("RAG_DEBUG", "➡️ Calling retrieve...")
            val result = SimpleRAGEngine.retrieve(
                bookId,
                chapterName,
                userMessage,
                topK = 3
            )

            if (result.isBlank()) {
                Log.e("RAG_DEBUG", "❌ Retrieved context is EMPTY")
            } else {
                Log.d("RAG_DEBUG", "✅ Retrieved context SUCCESS")
                Log.d("RAG_DEBUG", "Retrieved preview: ${result.take(200)}")
            }

            result
        } else {
            ""
        }

        val finalContext = if (retrievedContext.isNotBlank()) {
            retrievedContext
        } else {
            Log.w("RAG_DEBUG", "⚠️ Using FALLBACK content")
            content?.take(1200) ?: "Không có nội dung chương."
        }

        val sourceLabel = if (retrievedContext.isNotBlank()) {
            "[Ngữ cảnh được truy xuất bằng RAG]"
        } else {
            "[Fallback: nội dung đầu chương]"
        }

        Log.d("RAG_DEBUG", "========== END PROMPT BUILD ==========")

        return """
Bạn là trợ lý AI trong ứng dụng đọc sách LibraryApp.

Sách: ${book?.title ?: "Không rõ"} — Tác giả: ${book?.author ?: "Không rõ"}
Chương: $chapterName

$sourceLabel
$finalContext

Yêu cầu:
- Trả lời dựa vào nội dung trên
- Nếu không có thông tin: nói "Nội dung chương không đề cập điều này"
- Trả lời ngắn gọn, dễ hiểu

Câu hỏi: $userMessage
        """.trimIndent()
    }
}