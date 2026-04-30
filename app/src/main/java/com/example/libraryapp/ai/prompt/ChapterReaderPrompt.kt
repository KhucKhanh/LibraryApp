package com.example.libraryapp.ai.prompt

import android.util.Log
import com.example.libraryapp.ai.AIContextManager
import com.example.libraryapp.ai.RetrievedChunk
import com.example.libraryapp.ai.SimpleRAGEngine

object ChapterReaderPrompt {

    // Ngưỡng phân biệt chapter ngắn / dài
    private const val CHAPTER_SHORT_THRESHOLD = 3000
    private const val CHAPTER_DIRECT_LIMIT = 6000

    // Từ khóa detect cross-book
    private val CROSS_BOOK_KEYWORDS = listOf(
        "giống", "giong",
        "khác", "khac",
        "so sánh", "so sanh",
        "compare",
        "khác nhau", "khac nhau",
        "giống nhau", "giong nhau",
        "với cuốn", "voi cuon",
        "và cuốn", "va cuon"
    )

    // Từ khóa cần RAG bổ sung dù không cross-book
    private val DEEP_QUERY_KEYWORDS = listOf(
        "tại sao", "tai sao",
        "vì sao", "vi sao",
        "giải thích", "giai thich",
        "ý nghĩa", "y nghia",
        "phân tích", "phan tich",
        "chủ đề", "chu de"
    )

    suspend fun build(
        context: AIContextManager.Snapshot,
        userMessage: String
    ): String {

        val book = context.book
        val chapterName = context.chapter ?: "Không rõ"
        val bookId = book?.id ?: ""
        val lowerMsg = userMessage.lowercase()
        val contentLength = context.chapterContent?.length ?: 0
        val isShortChapter = contentLength <= CHAPTER_SHORT_THRESHOLD

        Log.d("RAG_DEBUG", "========== ChapterReaderPrompt ==========")
        Log.d("RAG_DEBUG", "BookId: $bookId | Chapter: $chapterName")
        Log.d("RAG_DEBUG", "User question: $userMessage")
        Log.d("RAG_DEBUG", "Content length: $contentLength | isShort: $isShortChapter")

        // Index chapter nếu chưa có
        if (!context.chapterContent.isNullOrBlank()) {
            Log.d("RAG_DEBUG", "Calling indexChapter...")
            SimpleRAGEngine.indexChapter(bookId, chapterName, context.chapterContent)
            Log.d("RAG_DEBUG", "indexChapter done")
        } else {
            Log.w("RAG_DEBUG", "chapterContent null/blank, skip index")
        }

        val isCrossBook = CROSS_BOOK_KEYWORDS.any { lowerMsg.contains(it) }
        val isDeepQuery = DEEP_QUERY_KEYWORDS.any { lowerMsg.contains(it) }

        // Chapter ngắn → không cần RAG trừ cross-book hoặc deep query
        // Chapter dài → luôn cần RAG để bù phần bị cắt
        val useRAG = isCrossBook || isDeepQuery || !isShortChapter

        Log.d("RAG_DEBUG", "isCrossBook=$isCrossBook | isDeepQuery=$isDeepQuery | isShortChapter=$isShortChapter | useRAG=$useRAG")

        // ── Phần 1: Chapter content ──────────────────────────────────────────
        val chapterContext = when {
            context.chapterContent.isNullOrBlank() -> ""
            isShortChapter -> context.chapterContent               // full nếu ngắn
            else -> context.chapterContent.take(CHAPTER_DIRECT_LIMIT) // cắt nếu dài
        }

        val contentNote = when {
            context.chapterContent.isNullOrBlank() ->
                "[Không có nội dung chương]"
            isShortChapter ->
                "[Toàn bộ nội dung chương — $contentLength ký tự]"
            else ->
                "[Chương dài ($contentLength ký tự) — hiển thị $CHAPTER_DIRECT_LIMIT ký tự đầu, các đoạn liên quan bổ sung qua RAG bên dưới]"
        }

        // ── Phần 2: RAG ──────────────────────────────────────────────────────
        val retrievedChunks: List<RetrievedChunk> = if (useRAG) {
            try {
                // Query kết hợp: nội dung chapter + câu hỏi user
                val ragQuery = buildString {
                    if (!context.chapterContent.isNullOrBlank()) {
                        append(context.chapterContent.take(300))
                        append("\n\n")
                    }
                    append(userMessage)
                }

                Log.d("RAG_DEBUG", "RAG query (preview): ${ragQuery.take(100)}...")

                val result = SimpleRAGEngine.retrieve(
                    query = ragQuery,
                    currentBookId = bookId,
                    topK = if (isCrossBook) 6 else 3,
                    filterToCurrentBook = !isCrossBook
                )

                // Cross-book: giới hạn 2 chunk/sách để tránh 1 sách chiếm hết
                val final = if (isCrossBook) {
                    result.groupBy { it.bookId }
                        .values
                        .flatMap { it.take(2) }
                        .sortedByDescending { it.score }
                        .take(4)
                } else {
                    result
                }

                Log.d("RAG_DEBUG", "Retrieved ${final.size} chunks (crossBook=$isCrossBook)")
                final.forEach {
                    Log.d("RAG_DEBUG", "  score=${it.score} | book=${it.bookId} | chapter=${it.chapter}")
                }

                final
            } catch (e: Exception) {
                Log.e("RAG_DEBUG", "Retrieve FAILED: ${e.message}")
                emptyList()
            }
        } else {
            Log.d("RAG_DEBUG", "RAG skipped (short chapter + simple query)")
            emptyList()
        }

        // ── Build final context ──────────────────────────────────────────────
        val finalContext = buildString {
            append(contentNote)
            append("\n\n")

            if (chapterContext.isNotBlank()) {
                append("[Nội dung chương đang đọc — $chapterName]\n")
                append(chapterContext)
            } else {
                append("[Không có nội dung chương]")
            }

            if (retrievedChunks.isNotEmpty()) {
                append("\n\n[Đoạn liên quan được truy xuất — ${retrievedChunks.size} đoạn]\n")
                append(
                    retrievedChunks.joinToString("\n\n---\n\n") { chunk ->
                        "[${chunk.bookId} — ${chunk.chapter} | score=${"%.2f".format(chunk.score)}]\n${chunk.text}"
                    }
                )
            }
        }

        val sourceLabel = when {
            retrievedChunks.isNotEmpty() && isShortChapter ->
                "[Full chapter content + RAG ${retrievedChunks.size} chunks]"
            retrievedChunks.isNotEmpty() ->
                "[Partial chapter content + RAG ${retrievedChunks.size} chunks]"
            isShortChapter ->
                "[Full chapter content — không cần RAG]"
            else ->
                "[Partial chapter content — RAG không tìm được kết quả]"
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
- Luôn trả lời dựa trên nội dung có sẵn, dù ngắn hay chưa đầy đủ
- Chỉ nói "Nội dung không đề cập điều này" khi context hoàn toàn trống

Câu hỏi: $userMessage
        """.trimIndent()
    }
}