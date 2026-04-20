package com.example.libraryapp.data.remote

import com.example.libraryapp.model.Message
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseChatRepository {

    private val db = FirebaseFirestore.getInstance()

    fun saveMessage(
        userId: String,
        chatId: String,
        message: Message
    ) {
        val data = hashMapOf(
            "text" to message.text,
            "isUser" to message.isUser,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .add(data)
    }
}