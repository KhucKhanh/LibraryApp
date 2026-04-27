package com.example.libraryapp.data

import com.example.libraryapp.model.Book
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

    fun getLibraryBooks(
        libraryId: String,
        callback: (List<Book>) -> Unit
    ) {
        libRef().document(libraryId).get()
            .addOnSuccessListener { doc ->

                val ids = doc.get("books") as? List<String> ?: emptyList()

                db.collection("books")
                    .get()
                    .addOnSuccessListener { result ->

                        val allBooks = result.map {
                            Book(
                                id = it.id,
                                title = it.getString("title") ?: "",
                                author = it.getString("author") ?: "",
                                imageUrl = it.getString("imageUrl") ?: "",
                                description = it.getString("description") ?: ""
                            )
                        }

                        callback(allBooks.filter { it.id in ids })
                    }
            }
    }

    fun getLibraries(callback: (List<Library>) -> Unit) {
        libRef().get().addOnSuccessListener { result ->
            val libraries = result.map {
                Library(
                    id = it.id,
                    name = it.getString("name") ?: "",
                    books = it.get("books") as? List<String> ?: emptyList(),
                    createdAt = it.getTimestamp("createdAt")?.seconds ?: 0L // THÊM
                )
            }

            val sorted = libraries.sortedWith(
                compareByDescending<Library> { it.id == "liked" }
                    .thenBy { it.createdAt }
            )
            callback(sorted)
        }
    }

    fun removeBook(libraryId: String, bookId: String) {

        val ref = libRef().document(libraryId)

        ref.get().addOnSuccessListener { doc ->

            val list = doc.get("books") as? List<String> ?: emptyList()

            ref.update("books", list - bookId)
        }
    }

    fun toggleLiked(bookId: String, callback: (Boolean) -> Unit) {

        val ref = libRef().document("liked")

        ref.get().addOnSuccessListener { doc ->

            val list = doc.get("books") as? List<String> ?: emptyList()

            val isLiked = list.contains(bookId)

            val newList = if (isLiked) list - bookId else list + bookId

            ref.set(
                mapOf(
                    "name" to "Liked",
                    "books" to newList,
                    "system" to true
                )
            )

            callback(!isLiked)
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

    fun createLibrary(name: String, callback: (Boolean) -> Unit) {
        val newDoc = libRef().document() // auto-generate ID
        newDoc.set(
            mapOf(
                "name" to name,
                "books" to emptyList<String>(),
                "system" to false,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
        ).addOnSuccessListener {
            callback(true)
        }.addOnFailureListener {
            callback(false)
        }
    }

    fun deleteLibrary(libraryId: String, callback: (Boolean) -> Unit) {
        libRef().document(libraryId)
            .delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun renameLibrary(libraryId: String, newName: String, callback: (Boolean) -> Unit) {
        libRef().document(libraryId)
            .update("name", newName)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun addBookToLibrary(libraryId: String, bookId: String) {
        val ref = libRef().document(libraryId)
        ref.get().addOnSuccessListener { doc ->
            val list = doc.get("books") as? List<String> ?: emptyList()
            if (!list.contains(bookId)) {
                ref.update("books", list + bookId)
            }
        }
    }

}