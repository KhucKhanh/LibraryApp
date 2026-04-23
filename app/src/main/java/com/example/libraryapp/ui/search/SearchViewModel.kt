package com.example.libraryapp.ui.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.libraryapp.model.Book
import com.google.firebase.firestore.FirebaseFirestore

class SearchViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    val books = MutableLiveData<List<Book>>()

    private var allBooks = listOf<Book>() // 🔥 cache data

    fun getAllBooks() {
        db.collection("books")
            .get()
            .addOnSuccessListener { result ->
                allBooks = result.documents.map { doc ->
                    doc.toObject(Book::class.java)!!.copy(id = doc.id) // ✅
                }
            }
    }

    fun searchBooks(query: String) {
        if (query.isEmpty()) {
            books.value = emptyList()
            return
        }

        val filtered = allBooks.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.author.contains(query, ignoreCase = true)
        }

        books.value = filtered
    }
}