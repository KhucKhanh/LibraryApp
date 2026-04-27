package com.example.libraryapp.ai

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreRAGRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun saveChunks(
        bookId: String,
        chapter: String,
        chunks: List<String>
    ) {
        val existing = db.collection("book_chunks")
            .whereEqualTo("bookId", bookId)
            .whereEqualTo("chapter", chapter)
            .get()
            .await()

        if (!existing.isEmpty) {
            android.util.Log.d("RAG_DEBUG", "Chunks already exist, skip saving")
            return
        }

        for ((index, chunk) in chunks.withIndex()) {
            val embedding = EmbeddingClient.embed(chunk) ?: continue

            val data = ChunkData(
                bookId = bookId,
                chapter = chapter,
                text = chunk,
                embedding = embedding,
                chunkIndex = index
            )

            db.collection("book_chunks")
                .document("$bookId-$chapter-$index")
                .set(data)
                .await()

            android.util.Log.d("RAG_DEBUG", "Saved chunk $index")
        }
    }

    suspend fun getChunks(bookId: String, chapter: String): List<ChunkData> {
        val snapshot = db.collection("book_chunks")
            .whereEqualTo("bookId", bookId)
            .whereEqualTo("chapter", chapter)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.toObject(ChunkData::class.java)
                data
            } catch (e: Exception) {
                null
            }
        }
    }

}