package com.example.libraryapp.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LibrarySeeder {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun createLikedLibrary() {

        val user = auth.currentUser ?: return
        val userId = user.uid

        val ref = db.collection("users")
            .document(userId)
            .collection("libraries")
            .document("liked")

        ref.get().addOnSuccessListener { doc ->

            if (!doc.exists()) {

                val data = hashMapOf(
                    "name" to "Liked",
                    "books" to emptyList<String>(),
                    "system" to true
                )

                ref.set(data)
            }
        }
    }
}