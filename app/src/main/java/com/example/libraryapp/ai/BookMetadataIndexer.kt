package com.example.libraryapp.ai

import android.util.Log
import com.example.libraryapp.model.Book
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BookMetadataIndexer {

    private var indexed = false

    suspend fun indexAllIfNeeded(books: List<Book>) {
        if (indexed || books.isEmpty()) return
        indexed = true

        Log.d("RAG_DEBUG", "BookMetadataIndexer: indexing ${books.size} books")

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("RAG_DEBUG", "BookMetadataIndexer: indexing ${books.size} books (background)")
            books.filter { !it.description.isNullOrBlank() }
                .map { book ->
                    async {
                        FirestoreRAGRepository.saveBookMetadata(
                            bookId = book.id,
                            title = book.title ?: "",
                            author = book.author ?: "",
                            description = book.description!!
                        )
                    }
                }
                .awaitAll()
            Log.d("RAG_DEBUG", "BookMetadataIndexer: done")
        }

        Log.d("RAG_DEBUG", "BookMetadataIndexer: done")
    }

    fun reset() { indexed = false }
}