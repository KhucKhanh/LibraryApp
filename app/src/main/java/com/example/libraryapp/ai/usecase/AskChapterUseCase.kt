package com.example.libraryapp.ai.usecase

import com.example.libraryapp.ai.AIContextManager
import com.example.libraryapp.ai.SimpleRAGEngine

object AskChapterUseCase {

    suspend fun buildPrompt(userMessage: String): String {
        val chapterContent = AIContextManager.currentChapterContent
        val chapterName = AIContextManager.currentChapter
        val bookId = AIContextManager.currentBook?.id ?: ""

        android.util.Log.d("RAG_DEBUG", "=== AskChapterUseCase ===")
        android.util.Log.d("RAG_DEBUG", "chapterName: $chapterName")
        android.util.Log.d("RAG_DEBUG", "contentLength: ${chapterContent?.length ?: 0}")
        android.util.Log.d("RAG_DEBUG", "content preview: ${chapterContent?.take(100) ?: "NULL"}")
        android.util.Log.d("RAG_DEBUG", "Retrieved from Firestore")

        val retrievedContext = if (!chapterContent.isNullOrBlank()) {
            SimpleRAGEngine.indexChapter(bookId, chapterName ?: "", chapterContent)
            SimpleRAGEngine.retrieve(
                bookId,
                chapterName ?: "",
                userMessage
            )
        } else ""

        val contextToUse = if (retrievedContext.isNotBlank()) retrievedContext
        else chapterContent?.take(1500) ?: "Không có nội dung chương."

        val sourceLabel = if (retrievedContext.isNotBlank())
            "[Đoạn liên quan được truy xuất — Semantic RAG]"
        else
            "[Fallback: nội dung đầu chương]"

        return """
Bạn là trợ lý đọc sách trong LibraryApp.
Nhiệm vụ: Trả lời câu hỏi dựa HOÀN TOÀN vào nội dung được cung cấp.
Nếu không có trong nội dung, hãy nói: "Nội dung chương không đề cập đến điều này."

Chương: ${chapterName ?: "Không rõ"}

$sourceLabel
$contextToUse

Câu hỏi: $userMessage
        """.trimIndent()
    }
}