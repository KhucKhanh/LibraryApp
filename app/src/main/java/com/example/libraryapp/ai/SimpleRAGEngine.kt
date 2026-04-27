package com.example.libraryapp.ai

import kotlin.math.sqrt

object SimpleRAGEngine {

    private var storedChunks: List<Pair<String, List<Float>>> = emptyList()
    private var lastKey: String = ""

    suspend fun indexChapter(bookId: String, chapterTitle: String, content: String) {
        val key = "$bookId-$chapterTitle"
        if (key == lastKey) return
        lastKey = key

        val chunks = chunkContent(content)
        FirestoreRAGRepository.saveChunks(bookId, chapterTitle, chunks)
    }

    suspend fun retrieve(
        bookId: String,
        chapter: String,
        query: String,
        topK: Int = 2
    ): String {

        val chunks = FirestoreRAGRepository.getChunks(bookId, chapter)

        if (chunks.isEmpty()) return ""

        val queryVector = EmbeddingClient.embed(query)
            ?: return fallbackRetrieveFromChunks(query, chunks, topK)

        return chunks
            .map { chunk ->
                val score = cosineSimilarity(queryVector, chunk.embedding)
                Pair(chunk.text, score)
            }
            .sortedByDescending { it.second }
            .distinctBy { it.first }
            .take(topK)
            .joinToString("\n\n---\n\n") { it.first }
    }

    fun clear() {
        storedChunks = emptyList()
        lastKey = ""
    }

    private fun chunkContent(content: String): List<String> {
        val sentences = content.split(Regex("(?<=[.!?])\\s+"))

        val chunks = mutableListOf<String>()
        var current = ""

        for (sentence in sentences) {
            if ((current + sentence).length < 1000) {
                current += " $sentence"
            } else {
                chunks.add(current.trim())
                current = sentence
            }
        }

        if (current.isNotEmpty()) {
            chunks.add(current.trim())
        }

        return chunks
    }

    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.size != b.size) return 0f
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return if (normA == 0f || normB == 0f) 0f
        else dot / (sqrt(normA) * sqrt(normB))
    }

    private fun tokenize(text: String): Set<String> =
        text.lowercase().split(Regex("[\\s,\\.!?;:\"'()\\[\\]]+"))
            .filter { it.length > 1 }.toSet()

    private fun jaccardSimilarity(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val intersection = a.intersect(b).size.toFloat()
        val union = a.union(b).size.toFloat()
        return if (union == 0f) 0f else intersection / union
    }

    private fun fallbackRetrieveFromChunks(
        query: String,
        chunks: List<ChunkData>,
        topK: Int
    ): String {
        val queryWords = tokenize(query)

        return chunks
            .map { chunk ->
                val score = jaccardSimilarity(queryWords, tokenize(chunk.text))
                Pair(chunk.text, score)
            }
            .sortedByDescending { it.second }
            .take(topK)
            .joinToString("\n\n---\n\n") { it.first }
    }

}