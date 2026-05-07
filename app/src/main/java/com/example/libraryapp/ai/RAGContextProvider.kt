package com.example.libraryapp.ai

import android.util.Log

object RAGContextProvider {

    enum class RAGMode {
        CURRENT_BOOK,
        CROSS_BOOK,
        GLOBAL
    }

    data class RAGResult(
        val chunks: List<RetrievedChunk>,
        val mode: RAGMode,
        val contextBlock: String
    )

    private val CROSS_BOOK_KEYWORDS = listOf(
        "giống", "khác", "so sánh", "compare",
        "khác nhau", "giống nhau", "với cuốn", "và cuốn"
    )
    private val DEEP_QUERY_KEYWORDS = listOf(
        "tại sao", "vì sao", "giải thích",
        "ý nghĩa", "phân tích", "chủ đề"
    )

    fun detectMode(
        userMessage: String,
        hasCurrentBook: Boolean
    ): RAGMode {
        val lower = userMessage.lowercase()
        return when {
            CROSS_BOOK_KEYWORDS.any { lower.contains(it) } -> RAGMode.CROSS_BOOK
            !hasCurrentBook -> RAGMode.GLOBAL
            else -> RAGMode.CURRENT_BOOK
        }
    }

    fun needsRAG(userMessage: String, mode: RAGMode): Boolean {
        val lower = userMessage.lowercase()
        val isDeep = DEEP_QUERY_KEYWORDS.any { lower.contains(it) }
        // GLOBAL luôn cần RAG, CROSS_BOOK luôn cần, CURRENT_BOOK chỉ khi deep query
        return mode != RAGMode.CURRENT_BOOK || isDeep
    }

    suspend fun getContext(
        userMessage: String,
        currentBookId: String = "",
        chapter: String? = null,
        chapterContent: String? = null,
        mode: RAGMode = RAGMode.GLOBAL
    ): RAGResult {

        Log.d("RAG_DEBUG", "RAGContextProvider | mode=$mode | bookId=$currentBookId")

        // Index chapter nếu có (ChapterReader)
        if (!chapterContent.isNullOrBlank() && chapter != null && currentBookId.isNotBlank()) {
            SimpleRAGEngine.indexChapter(currentBookId, chapter, chapterContent)
        }

        val topK = when (mode) {
            RAGMode.CROSS_BOOK  -> 4
            RAGMode.GLOBAL      -> 3
            RAGMode.CURRENT_BOOK -> 2
        }

        val filterToCurrentBook = mode == RAGMode.CURRENT_BOOK

        val chunks = try {
            android.util.Log.d("CHAT_DEBUG", "Calling retrieve...")
            val raw = SimpleRAGEngine.retrieve(
                query = userMessage,
                currentBookId = currentBookId,
                topK = topK,
                filterToCurrentBook = filterToCurrentBook
            )
            android.util.Log.d("CHAT_DEBUG", "retrieve done: ${raw.size} chunks")
            raw

            // Cross-book: giới hạn 2 chunk/sách
            if (mode == RAGMode.CROSS_BOOK) {
                raw.groupBy { it.bookId }
                    .values
                    .flatMap { it.take(2) }
                    .sortedByDescending { it.score }
                    .take(4)
            } else {
                raw
            }
        } catch (e: Exception) {
            android.util.Log.e("CHAT_DEBUG", "retrieve FAILED: ${e.message}")
            emptyList()
        }

        android.util.Log.d("CHAT_DEBUG", "RAGContextProvider DONE")

        val contextBlock = buildContextBlock(chunks, mode)

        return RAGResult(
            chunks = chunks,
            mode = mode,
            contextBlock = contextBlock
        )
    }

    private fun buildContextBlock(
        chunks: List<RetrievedChunk>,
        mode: RAGMode
    ): String {
        if (chunks.isEmpty()) return ""

        val header = when (mode) {
            RAGMode.CURRENT_BOOK -> "[Đoạn nội dung liên quan từ sách đang xem — ${chunks.size} đoạn]"
            RAGMode.CROSS_BOOK   -> "[Đoạn liên quan từ các sách khác — ${chunks.size} đoạn]"
            RAGMode.GLOBAL       -> "[Đoạn liên quan từ thư viện — ${chunks.size} đoạn]"
        }

        val body = chunks.joinToString("\n\n---\n\n") { chunk ->
            "[${chunk.bookId} — ${chunk.chapter}]\n${chunk.text.take(300)}"
        }

        return "$header\n$body"
    }
}