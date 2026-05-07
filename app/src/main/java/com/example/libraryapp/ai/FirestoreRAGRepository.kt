package com.example.libraryapp.ai

import android.util.Log
import com.example.libraryapp.data.remote.EmbeddingClient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirestoreRAGRepository {

    private val db = FirebaseFirestore.getInstance()

    // ── Cache in-memory ──────────────────────────────────────────────────────
    internal val chunksCache = mutableListOf<ChunkData>()
    internal var cacheLoaded = false
    private val indexedChapters = mutableSetOf<String>()

    suspend fun getChunkCount(bookId: String, chapter: String): Int {
        val key = "$bookId-$chapter"
        if (key in indexedChapters) return 1
        return try {
            val snapshot = withContext(Dispatchers.IO) {
                db.collection("book_chunks")
                    .whereEqualTo("bookId", bookId)
                    .whereEqualTo("chapter", chapter)
                    .get().await()
            }
            val count = snapshot.size()
            if (count > 0) indexedChapters.add(key)
            count
        } catch (e: Exception) {
            Log.e("RAG_DEBUG", "getChunkCount failed: ${e.message}")
            0
        }
    }

    suspend fun saveChunks(bookId: String, chapter: String, chunks: List<String>) {
        Log.d("RAG_DEBUG", "saveChunks START")
        val key = "$bookId-$chapter"

        for ((index, chunk) in chunks.withIndex()) {
            if (chunk.length < 100) continue

            val embedding = withContext(Dispatchers.IO) {
                EmbeddingClient.embed(chunk)
            } ?: continue

            val data = ChunkData(
                bookId = bookId,
                chapter = chapter,
                text = chunk,
                embedding = embedding,
                chunkIndex = index
            )

            chunksCache.add(data)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.collection("book_chunks")
                        .document("$bookId-$chapter-$index")
                        .set(data)
                        .await()
                } catch (e: Exception) {
                    Log.e("RAG_DEBUG", "Firestore save failed: ${e.message}")
                }
            }
        }

        indexedChapters.add(key)
        Log.d("RAG_DEBUG", "saveChunks END (cache size=${chunksCache.size})")
    }

    suspend fun getAllChunks(): List<ChunkData> {
        if (cacheLoaded) {
            Log.d("RAG_DEBUG", "getAllChunks from cache: ${chunksCache.size}")
            return chunksCache.toList()
        }

        Log.d("RAG_DEBUG", "getAllChunks called (first time)")
        return withContext(Dispatchers.IO) {
            val snapshot = db.collection("book_chunks").get().await()
            Log.d("RAG_DEBUG", "getAllChunks docs: ${snapshot.documents.size}")
            val loaded = snapshot.documents.mapNotNull {
                try { it.toObject(ChunkData::class.java) } catch (e: Exception) { null }
            }
            chunksCache.clear()
            chunksCache.addAll(loaded)
            cacheLoaded = true
            chunksCache.toList()
        }
    }

    fun clearCache() {
        chunksCache.clear()
        indexedChapters.clear()
        cacheLoaded = false
    }


    suspend fun saveBookMetadata(bookId: String, title: String, author: String, description: String) {
        val key = "meta-$bookId"
        if (key in indexedChapters) return  // tái dùng set đã có

        // Kiểm tra Firestore
        val existing = try {
            val snap = withContext(Dispatchers.IO) {
                db.collection("book_chunks")
                    .whereEqualTo("bookId", bookId)
                    .whereEqualTo("chapter", "__metadata__")
                    .get().await()
            }
            snap.size()
        } catch (e: Exception) { 0 }

        if (existing > 0) {
            indexedChapters.add(key)
            return
        }

        val metaText = "Sách: $title. Tác giả: $author. Nội dung: $description"
        if (metaText.length < 20) return

        val embedding = withContext(Dispatchers.IO) {
            EmbeddingClient.embed(metaText)
        } ?: return

        val data = ChunkData(
            bookId = bookId,
            chapter = "__metadata__",
            text = metaText,
            embedding = embedding,
            chunkIndex = 0
        )

        chunksCache.add(data)
        indexedChapters.add(key)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("book_chunks")
                    .document("$bookId-metadata")
                    .set(data)
                    .await()
            } catch (e: Exception) {
                Log.e("RAG_DEBUG", "saveBookMetadata failed: ${e.message}")
            }
        }
    }
}