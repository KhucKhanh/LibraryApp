package com.example.libraryapp.recommendation

import com.example.libraryapp.model.Book

object HybridRecommendationEngine {

    /**
     * Hybrid Recommendation = 60% Behavior + 40% Content
     *
     * BehaviorScore: dựa trên categoryScore tích lũy từ hành vi user
     * ContentScore : category match (50%) + author (30%) + tags (20%)
     *
     * @param books        sách chưa đọc
     * @param scoreMap     categoryScore của user (raw từ Firestore)
     * @param recentBooks  sách user đã đọc gần đây
     */
    fun rank(
        books: List<Book>,
        scoreMap: Map<String, Long>,
        recentBooks: List<Book>
    ): List<Book> {
        if (books.isEmpty()) return emptyList()

        // =====================
        // BEHAVIOR (60%)
        // =====================
        val normalizedScoreMap = scoreMap
            .mapKeys { it.key.trim().lowercase() }

        val totalScore = normalizedScoreMap.values.sum().toFloat().coerceAtLeast(1f)

        // Tỉ lệ 0..1 cho từng category
        val categoryWeight: Map<String, Float> = normalizedScoreMap
            .mapValues { it.value.toFloat() / totalScore }

        // =====================
        // CONTENT (40%)
        // =====================
        // Tần suất author user hay đọc
        val authorFreq = recentBooks
            .groupingBy { it.author.trim().lowercase() }
            .eachCount()
        val maxAuthorFreq = authorFreq.values.maxOrNull()?.toFloat() ?: 1f

        // Tần suất tags user hay gặp
        val tagFreq = recentBooks
            .flatMap { it.tags }
            .groupingBy { it.trim().lowercase() }
            .eachCount()
        val maxTagFreq = tagFreq.values.maxOrNull()?.toFloat() ?: 1f

        // =====================
        // SCORE TỪNG SÁCH
        // =====================
        return books
            .map { book ->
                val normalizedCategory = book.category.trim().lowercase()

                // --- Behavior ---
                val behaviorScore = categoryWeight[normalizedCategory] ?: 0f

                // --- Content ---
                // Category match: có trong history user không?
                val categoryMatch = if (categoryWeight.containsKey(normalizedCategory)) 1f else 0f

                // Author score: normalize về 0..1
                val authorScore = (authorFreq[book.author.trim().lowercase()]?.toFloat() ?: 0f) / maxAuthorFreq

                // Tag score: trung bình các tag của sách
                val tagScore = if (book.tags.isEmpty()) 0f else {
                    book.tags.sumOf {
                        tagFreq[it.trim().lowercase()]?.toDouble() ?: 0.0
                    }.toFloat() / (book.tags.size * maxTagFreq)
                }

                val contentScore = 0.3f * authorScore + 0.2f * tagScore
                // --- Hybrid Final ---
                val finalScore = 0.6f * behaviorScore + 0.4f * contentScore

                Pair(book, finalScore)
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}