package com.example.libraryapp.data

import android.util.Log
import com.example.libraryapp.model.Chapter
import com.google.firebase.firestore.FirebaseFirestore

class ChapterRepository {

    private val db = FirebaseFirestore.getInstance()

    fun getChaptersByBook(bookId: String, onResult: (List<Chapter>) -> Unit) {
        db.collection("books")
            .document(bookId)
            .collection("chapters")
            .orderBy("order")
            .get()
            .addOnSuccessListener { result ->
                val chapters = result.map { doc ->
                    doc.toObject(Chapter::class.java).copy(id = doc.id)
                }

                Log.d("REPO", "Loaded ${chapters.size} chapters")

                onResult(chapters)
            }
            .addOnFailureListener {
                Log.e("REPO", "Error", it)
                onResult(emptyList())
            }
    }
}