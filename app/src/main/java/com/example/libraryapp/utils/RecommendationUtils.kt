package com.example.libraryapp.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object RecommendationUtils {

    private val db by lazy { FirebaseFirestore.getInstance() }
    /**
     * Implicit feedback scoring:
     *
     * +1 Mở BookDetail          → xem qua
     * +2 Hoàn thành 1 chapter   → đọc liên tục
     * +3 Bắt đầu đọc chapter    → quan tâm thật sự
     * +4 Like / Thêm vào thư viện → yêu thích
     * +5 Hoàn thành sách         → tín hiệu mạnh nhất
     */
    fun addCategoryScore(category: String?, score: Int) {
        if (category.isNullOrBlank()) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Normalize: lowercase + trim → tránh trùng key kiểu "Fantasy" vs "fantasy"
        val normalizedCategory = category.trim().lowercase()

        val userRef = db.collection("users").document(userId)
        val field = "categoryScore.$normalizedCategory"

        userRef.set(
            mapOf(field to com.google.firebase.firestore.FieldValue.increment(score.toLong())),
            SetOptions.merge()
        )
    }
}