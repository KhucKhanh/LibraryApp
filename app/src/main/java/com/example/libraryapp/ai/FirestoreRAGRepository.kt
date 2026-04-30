package com.example.libraryapp.ai

import android.util.Log
import com.example.libraryapp.data.remote.EmbeddingClient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirestoreRAGRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getChunkCount(bookId: String, chapter: String): Int {
        return try {
            val snapshot = withContext(Dispatchers.IO) {
                db.collection("book_chunks")
                    .whereEqualTo("bookId", bookId)
                    .whereEqualTo("chapter", chapter)
                    .get()
                    .await()
            }
            snapshot.size()
        } catch (e: Exception) {
            Log.e("RAG_DEBUG", "getChunkCount failed: ${e.message}")
            0
        }
    }

    suspend fun saveChunks(bookId: String, chapter: String, chunks: List<String>) {
        Log.d("RAG_DEBUG", "saveChunks START")

        for ((index, chunk) in chunks.withIndex()) {
            Log.d("RAG_DEBUG", "Chunk $index, length=${chunk.length}")

            if (chunk.length < 100) {
                Log.w("RAG_DEBUG", "Skip short chunk")
                continue
            }

            val embedding = withContext(Dispatchers.IO) {
                EmbeddingClient.embed(chunk)
            }

            if (embedding == null) {
                Log.e("RAG_DEBUG", "Embedding FAILED at chunk $index")
                continue
            }

            Log.d("RAG_DEBUG", "Embedding OK (${embedding.size})")

            val data = ChunkData(
                bookId = bookId,
                chapter = chapter,
                text = chunk,
                embedding = embedding,
                chunkIndex = index
            )

            try {
                withContext(Dispatchers.IO) {
                    db.collection("book_chunks")
                        .document("$bookId-$chapter-$index")
                        .set(data)
                        .await()
                }
                Log.d("RAG_DEBUG", "Saved chunk $index")
            } catch (e: Exception) {
                Log.e("RAG_DEBUG", "Firestore save FAILED: ${e.message}")
            }
        }

        Log.d("RAG_DEBUG", "saveChunks END")
    }

    suspend fun getAllChunks(): List<ChunkData> {
        Log.d("RAG_DEBUG", "getAllChunks called")
        return withContext(Dispatchers.IO) {
            val snapshot = db.collection("book_chunks").get().await()
            Log.d("RAG_DEBUG", "getAllChunks docs: ${snapshot.documents.size}")
            snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(ChunkData::class.java) }
                catch (e: Exception) {
                    Log.e("RAG_DEBUG", "Parse error: ${e.message}")
                    null
                }
            }
        }
    }
}