package com.example.libraryapp.ai

import android.util.Log
import com.example.libraryapp.data.remote.EmbeddingClient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreRAGRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun saveChunks(
        bookId: String,
        chapter: String,
        chunks: List<String>
    ) {
        Log.d("RAG_DEBUG", "🔥 saveChunks START")

        val existing = db.collection("book_chunks")
            .whereEqualTo("bookId", bookId)
            .whereEqualTo("chapter", chapter)
            .get()
            .await()

        Log.d("RAG_DEBUG", "Existing docs: ${existing.documents.size}")

        // ✅ FIX CHÍNH Ở ĐÂY
        if (existing.documents.isNotEmpty()) {
            Log.d("RAG_DEBUG", "⚠️ Chunks already exist, skip saving")
            return
        }

        for ((index, chunk) in chunks.withIndex()) {

            Log.d("RAG_DEBUG", "➡️ Chunk $index, length=${chunk.length}")

            if (chunk.length < 100) {
                Log.w("RAG_DEBUG", "⚠️ Skip short chunk")
                continue
            }

            val embedding = EmbeddingClient.embed(chunk)

            if (embedding == null) {
                Log.e("RAG_DEBUG", "❌ Embedding FAILED at chunk $index")
                continue
            }

            Log.d("RAG_DEBUG", "✅ Embedding OK (${embedding.size})")

            val data = ChunkData(
                bookId = bookId,
                chapter = chapter,
                text = chunk,
                embedding = embedding,
                chunkIndex = index
            )

            try {
                db.collection("book_chunks")
                    .document("$bookId-$chapter-$index")
                    .set(data)
                    .await()

                Log.d("RAG_DEBUG", "✅ Saved chunk $index")

            } catch (e: Exception) {
                Log.e("RAG_DEBUG", "❌ Firestore save FAILED: ${e.message}")
            }
        }

        Log.d("RAG_DEBUG", "🔥 saveChunks END")
    }

    suspend fun getChunks(bookId: String, chapter: String): List<ChunkData> {
        Log.d("RAG_DEBUG", "📥 getChunks: $bookId - $chapter")

        val snapshot = db.collection("book_chunks")
            .whereEqualTo("bookId", bookId)
            .whereEqualTo("chapter", chapter)
            .get()
            .await()

        Log.d("RAG_DEBUG", "📊 Retrieved docs: ${snapshot.documents.size}")

        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(ChunkData::class.java)
            } catch (e: Exception) {
                Log.e("RAG_DEBUG", "❌ Parse error: ${e.message}")
                null
            }
        }
    }
}