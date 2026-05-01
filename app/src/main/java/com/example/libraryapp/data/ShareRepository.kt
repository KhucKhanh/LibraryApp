package com.example.libraryapp.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ShareRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val myId get() = auth.currentUser!!.uid

    /**
     * Chia sẻ sách tới một hoặc nhiều bạn bè.
     * Mỗi người nhận sẽ có 1 doc trong users/{friendId}/sharedBooks/
     */
    fun shareBookToFriends(
        bookId: String,
        bookTitle: String,
        friendIds: List<String>,
        onComplete: (success: Boolean) -> Unit
    ) {
        if (friendIds.isEmpty()) {
            onComplete(false)
            return
        }

        val batch = db.batch()

        friendIds.forEach { friendId ->
            val ref = db.collection("users")
                .document(friendId)
                .collection("sharedBooks")
                .document()

            batch.set(
                ref,
                mapOf(
                    "bookId"     to bookId,
                    "bookTitle"  to bookTitle,
                    "fromId"     to myId,
                    "timestamp"  to Timestamp.now(),
                    "read"       to false
                )
            )
        }

        batch.commit()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /**
     * Kiểm tra xem mình đã share cuốn sách này cho friend chưa
     * (dùng để highlight checkbox nếu muốn, hiện tại optional)
     */
    fun getAlreadySharedFriendIds(
        bookId: String,
        callback: (List<String>) -> Unit
    ) {
        // Không query ngược được hiệu quả → bỏ qua, mặc định chưa share
        callback(emptyList())
    }
}