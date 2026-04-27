package com.example.libraryapp.ai

object AIIntentDetector {

    enum class Intent {
        SUMMARIZE_BOOK,
        ASK_CHAPTER,
        RECOMMEND_BOOKS,
        GENERAL_CHAT
    }

    fun detect(message: String): Intent {
        val lower = message.lowercase()
        return when {
            lower.containsAny(
                "tóm tắt", "tóm lược", "cuốn này nói gì",
                "giới thiệu sách", "nội dung sách"
            ) -> Intent.SUMMARIZE_BOOK

            lower.containsAny(
                "chương này", "đoạn này", "giải thích",
                "nội dung vừa đọc", "đoạn vừa rồi"
            ) -> Intent.ASK_CHAPTER

            lower.containsAny(
                "gợi ý", "đề xuất", "sách tương tự",
                "sách khác", "nên đọc gì", "recommend"
            ) -> Intent.RECOMMEND_BOOKS

            else -> Intent.GENERAL_CHAT
        }
    }

    private fun String.containsAny(vararg keywords: String) =
        keywords.any { this.contains(it) }
}