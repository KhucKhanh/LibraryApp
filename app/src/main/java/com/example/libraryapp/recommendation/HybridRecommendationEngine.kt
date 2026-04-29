package com.example.libraryapp.recommendation

import com.example.libraryapp.model.Book

object HybridRecommendationEngine {

    /**
     * Hybrid Recommendation = 60% Behavior + 40% Content
     *
     * BehaviorScore: dựa trên categoryScore tích lũy từ hành vi user
     * ContentScore : category match (50%) + author (30%) + tags (20%)
     * Diversity    : tối đa 4 sách mỗi category trong kết quả
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

        val categoryWeight: Map<String, Float> = normalizedScoreMap
            .mapValues { it.value.toFloat() / totalScore }

        // =====================
        // CONTENT (40%)
        // =====================
        val authorFreq = recentBooks
            .groupingBy { it.author.trim().lowercase() }
            .eachCount()
        val maxAuthorFreq = authorFreq.values.maxOrNull()?.toFloat() ?: 1f

        val tagFreq = recentBooks
            .flatMap { it.tags }
            .groupingBy { it.trim().lowercase() }
            .eachCount()
        val maxTagFreq = tagFreq.values.maxOrNull()?.toFloat() ?: 1f

        val scored = books.map { book ->
            val normalizedCategory = book.category.trim().lowercase()

            val behaviorScore = categoryWeight[normalizedCategory] ?: 0f

            val categoryMatch = if (categoryWeight.containsKey(normalizedCategory)) 1f else 0f

            val authorScore = (authorFreq[book.author.trim().lowercase()]?.toFloat() ?: 0f) / maxAuthorFreq

            val tagScore = if (book.tags.isEmpty()) 0f else {
                book.tags.sumOf {
                    tagFreq[it.trim().lowercase()]?.toDouble() ?: 0.0
                }.toFloat() / (book.tags.size * maxTagFreq)
            }

            val contentScore = 0.5f * categoryMatch + 0.3f * authorScore + 0.2f * tagScore

            val finalScore = 0.6f * behaviorScore + 0.4f * contentScore

            Pair(book, finalScore)
        }
            .sortedByDescending { it.second }
            .map { it.first }

        val categoryCount = mutableMapOf<String, Int>()
        val diverse = scored.filter { book ->
            val cat = book.category.trim().lowercase()
            val count = categoryCount.getOrDefault(cat, 0)
            if (count < 5) {
                categoryCount[cat] = count + 1
                true
            } else false
        }

        return if (diverse.size >= 10) {
            diverse
        } else {
            val added = diverse.map { it.title }.toSet()
            val extra = scored.filter { it.title !in added }
            diverse + extra
        }
    }
}