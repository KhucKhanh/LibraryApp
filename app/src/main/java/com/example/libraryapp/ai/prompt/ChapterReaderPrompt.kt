package com.example.libraryapp.ai.prompt

import android.util.Log
import com.example.libraryapp.ai.AIContextManager
import com.example.libraryapp.ai.RetrievedChunk
import com.example.libraryapp.ai.SimpleRAGEngine

object ChapterReaderPrompt {

    suspend fun build(
        context: AIContextManager.Snapshot,
        userMessage: String
    ): String {

        val book = context.book
        val chapterName = context.chapter ?: "Không rõ"
        val bookId = book?.id ?: ""

        Log.d("RAG_DEBUG", "========== ChapterReaderPrompt ==========")
        Log.d("RAG_DEBUG", "BookId: $bookId")
        Log.d("RAG_DEBUG", "Chapter: $chapterName")
        Log.d("RAG_DEBUG", "User question: $userMessage")

        if (!context.chapterContent.isNullOrBlank()) {
            Log.d("RAG_DEBUG", "➡️ Calling indexChapter...")
            SimpleRAGEngine.indexChapter(bookId, chapterName, context.chapterContent)
            Log.d("RAG_DEBUG", "✅ indexChapter done")
        } else {
            Log.w("RAG_DEBUG", "⚠️ chapterContent null/blank, skip index")
        }

        val isCrossBook = listOf(
            "so sánh", "so sanh",
            "compare",
            "khác nhau", "khac nhau",
            "giống nhau", "giong nhau",
            "với cuốn", "voi cuon",
            "và cuốn", "va cuon"
        ).any { userMessage.lowercase().contains(it) }

        val retrievedChunks: List<RetrievedChunk> = try {
            val result = SimpleRAGEngine.retrieve(
                query = userMessage,
                currentBookId = bookId,
                topK = if (isCrossBook) 6 else 3,
                filterToCurrentBook = !isCrossBook
            )

            val final = if (isCrossBook) {
                result.groupBy { it.bookId }
                    .values
                    .flatMap { it.take(2) }
                    .sortedByDescending { it.score }
                    .take(4)
            } else {
                result
            }

            Log.d("RAG_DEBUG", "✅ Retrieved ${final.size} chunks (crossBook=$isCrossBook)")
            final
        } catch (e: Exception) {
            Log.e("RAG_DEBUG", "❌ Retrieve FAILED: ${e.message}")
            emptyList()
        }

        val finalContext: String
        val sourceLabel: String

        if (retrievedChunks.isNotEmpty()) {
            finalContext = retrievedChunks.joinToString("\n\n---\n\n") { chunk ->
                "[${chunk.bookId} — ${chunk.chapter}]\n${chunk.text}"
            }
            sourceLabel = "[Ngữ cảnh được truy xuất bằng RAG — ${retrievedChunks.size} đoạn]"
        } else {
            Log.w("RAG_DEBUG", "⚠️ RAG empty, dùng fallback content")
            finalContext = context.chapterContent?.take(1200)
                ?: "Không có nội dung chương."
            sourceLabel = "[Fallback: nội dung đầu chương hiện tại]"
        }

        Log.d("RAG_DEBUG", "Source: $sourceLabel")
        Log.d("RAG_DEBUG", "Context preview: ${finalContext.take(200)}")
        Log.d("RAG_DEBUG", "========== END PROMPT BUILD ==========")

        return """
Bạn là trợ lý AI trong ứng dụng đọc sách LibraryApp.

Sách đang đọc: ${book?.title ?: "Không rõ"} — Tác giả: ${book?.author ?: "Không rõ"}
Chương đang đọc: $chapterName

$sourceLabel
$finalContext

Yêu cầu:
- Trả lời NGẮN GỌN, tối đa 150 từ
- KHÔNG liệt kê lại toàn bộ nội dung, chỉ nêu ý chính
- Nếu context đến từ nhiều sách khác nhau, chỉ rõ nguồn
- Nếu không có thông tin: nói "Nội dung không đề cập điều này"

Câu hỏi: $userMessage
        """.trimIndent()
    }
}