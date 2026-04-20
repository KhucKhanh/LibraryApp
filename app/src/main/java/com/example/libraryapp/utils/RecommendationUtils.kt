package com.example.libraryapp.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object RecommendationUtils {

    private val db = FirebaseFirestore.getInstance()

    fun addCategoryScore(category: String?, score: Int) {

        if (category == null) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { doc ->

            val map = doc.get("categoryScore") as? Map<String, Long> ?: emptyMap()

            val current = map[category] ?: 0L
            val newScore = current + score

            val updated = map.toMutableMap()
            updated[category] = newScore

            userRef.set(
                mapOf("categoryScore" to updated),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }
    }
}