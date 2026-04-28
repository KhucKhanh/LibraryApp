package com.example.libraryapp.ai

import android.util.Log
import com.example.libraryapp.data.remote.EmbeddingClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.sqrt

object SimpleRAGEngine {

    private var lastKey: String = ""

    suspend fun indexChapter(bookId: String, chapterTitle: String, content: String) {
        val key = "$bookId-$chapterTitle"
        if (key == lastKey) return
        lastKey = key

        val chunks = chunkContent(content)
        withContext(Dispatchers.IO) {
            FirestoreRAGRepository.saveChunks(bookId, chapterTitle, chunks)
        }
    }

    suspend fun retrieve(
        query: String,
        currentBookId: String = "",
        topK: Int = 3
    ): List<RetrievedChunk> {

        Log.d("RAG_DEBUG", "🔍 retrieve() called, query=$query")

        val chunks = withContext(Dispatchers.IO) {
            FirestoreRAGRepository.getAllChunks()
        }

        Log.d("RAG_DEBUG", "📦 Total chunks fetched: ${chunks.size}")

        if (chunks.isEmpty()) {
            Log.e("RAG_DEBUG", "❌ No chunks found!")
            return emptyList()
        }

        val queryVector = try {
            Log.d("RAG_DEBUG", "⏳ Bắt đầu gọi EmbeddingClient.embed()")
            val v = withContext(Dispatchers.IO) {
                withTimeoutOrNull(5000L) {
                    Log.d("RAG_DEBUG", "⏳ Trong withTimeoutOrNull, gọi embed...")
                    EmbeddingClient.embed(query)
                }
            }
            Log.d("RAG_DEBUG", "⏳ Sau embed, v null? ${v == null}")
            if (v == null) {
                Log.e("RAG_DEBUG", "❌ QueryVector NULL — timeout hoặc embed() trả null")
            } else {
                Log.d("RAG_DEBUG", "🧮 QueryVector OK, size=${v.size}")
            }
            v
        } catch (e: Exception) {
            Log.e("RAG_DEBUG", "❌ Embed query EXCEPTION: ${e.message}", e)
            null
        }

        if (queryVector == null) return fallbackRetrieve(query, chunks, topK)

        val scored = chunks.map { chunk ->
            var score = cosineSimilarity(queryVector, chunk.embedding)
            if (chunk.bookId == currentBookId) score += 0.2f
            Log.d("RAG_DEBUG", "📊 score=$score | book=${chunk.bookId} | chapter=${chunk.chapter}")
            RetrievedChunk(
                text = chunk.text,
                bookId = chunk.bookId,
                chapter = chunk.chapter,
                score = score
            )
        }

        val result = scored
            .sortedByDescending { it.score }
            .distinctBy { it.text }
            .take(topK)

        Log.d("RAG_DEBUG", "✅ Final top ${result.size} chunks")
        result.forEach {
            Log.d("RAG_DEBUG", "🏆 book=${it.bookId} | chapter=${it.chapter} | score=${it.score}")
        }

        return result
    }

    fun clear() {
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

        if (current.isNotEmpty()) chunks.add(current.trim())
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

    private fun fallbackRetrieve(
        query: String,
        chunks: List<ChunkData>,
        topK: Int
    ): List<RetrievedChunk> {
        Log.w("RAG_DEBUG", "⚠️ Using Jaccard fallback")
        val queryWords = tokenize(query)
        val results = chunks
            .map { chunk ->
                val score = jaccardSimilarity(queryWords, tokenize(chunk.text))
                Log.d("RAG_DEBUG", "📊 Jaccard score=$score | book=${chunk.bookId} | chapter=${chunk.chapter}")
                RetrievedChunk(
                    text = chunk.text,
                    bookId = chunk.bookId,
                    chapter = chunk.chapter,
                    score = score
                )
            }
            .sortedByDescending { it.score }
            .distinctBy { it.text }
            .take(topK)

        Log.d("RAG_DEBUG", "✅ Fallback result: ${results.size} chunks")
        return results
    }
}