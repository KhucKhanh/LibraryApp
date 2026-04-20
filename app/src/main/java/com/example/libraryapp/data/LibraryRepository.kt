package com.example.libraryapp.data

import com.example.libraryapp.model.Library
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LibraryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun libRef() =
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("libraries")

    fun addBook(libraryId: String, bookId: String) {

        val ref = libRef().document(libraryId)

        ref.get().addOnSuccessListener { doc ->

            val list = doc.get("books") as? List<String> ?: emptyList()

            ref.update("books", list + bookId)
        }
    }

    fun getLibraries(callback: (List<Library>) -> Unit) {

        libRef().get().addOnSuccessListener { result ->

            val list = result.map { doc ->
                Library(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    books = doc.get("books") as? List<String> ?: emptyList()
                )
            }

            callback(list)
        }
    }

    fun isBookInLibrary(
        libraryId: String,
        bookId: String,
        callback: (Boolean) -> Unit
    ) {
        libRef().document(libraryId).get()
            .addOnSuccessListener { doc ->
                val list = doc.get("books") as? List<String> ?: emptyList()
                callback(list.contains(bookId))
            }
    }
}