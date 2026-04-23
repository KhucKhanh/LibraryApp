package com.example.libraryapp.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object RecommendationUtils {

    private val db = FirebaseFirestore.getInstance()

    fun addCategoryScore(category: String?, score: Int) {

        // ✅ FIX 1: chặn null + empty + blank
        if (category.isNullOrBlank()) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userRef = db.collection("users").document(userId)

        // ✅ FIX 2: dùng atomic update (KHÔNG cần get/set lại)
        val field = "categoryScore.$category"

        userRef.set(
            mapOf(field to com.google.firebase.firestore.FieldValue.increment(score.toLong())),
            SetOptions.merge()
        )
    }
}