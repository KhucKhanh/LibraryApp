package com.example.libraryapp.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.libraryapp.model.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    val books = MutableLiveData<List<Book>>()
    val recentBooks = MutableLiveData<List<Book>>()

    init {
        loadBooks()
    }

    // ===== LOAD ALL BOOKS =====
    private fun loadBooks() {
        db.collection("books")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.map { doc ->
                    val book = doc.toObject(Book::class.java)
                    book?.copy(id = doc.id) ?: Book(id = doc.id)
                }
                books.value = list
            }
    }

    // ===== LOAD RECENT BOOKS =====
    fun loadRecentBooks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("recent")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { result ->
                val bookIds = result.map { it.id }

                if (bookIds.isEmpty()) {
                    recentBooks.value = emptyList()
                    return@addOnSuccessListener
                }

                db.collection("books")
                    .whereIn(FieldPath.documentId(), bookIds)
                    .get()
                    .addOnSuccessListener { booksResult ->
                        val booksMap = booksResult.documents.associateBy { it.id }
                        val sortedBooks = bookIds.mapNotNull { id ->
                            booksMap[id]?.toObject(Book::class.java)?.copy(id = id)
                        }
                        recentBooks.value = sortedBooks
                    }
            }
    }
}