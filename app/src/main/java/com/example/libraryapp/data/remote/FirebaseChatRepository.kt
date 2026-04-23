package com.example.libraryapp.data.remote

import com.example.libraryapp.model.Chat
import com.example.libraryapp.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirebaseChatRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun messagesRef(userId: String, chatId: String) =
        db.collection("users")
            .document(userId)
            .collection("chats")
            .document(chatId)
            .collection("messages")

    private fun chatRef(userId: String, chatId: String) =
        db.collection("users")
            .document(userId)
            .collection("chats")
            .document(chatId)

    fun saveMessage(userId: String, chatId: String, message: Message) {
        val data = hashMapOf(
            "text" to message.text,
            "isUser" to message.isUser,
            "timestamp" to System.currentTimeMillis()
        )
        messagesRef(userId, chatId).add(data)
    }

    fun loadChatHistory(
        userId: String,
        chatId: String,
        onResult: (List<Message>) -> Unit
    ) {
        messagesRef(userId, chatId)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                val history = result.documents.mapNotNull { doc ->
                    val text = doc.getString("text") ?: return@mapNotNull null
                    val isUser = doc.getBoolean("isUser") ?: return@mapNotNull null
                    Message(text, isUser)
                }
                onResult(history)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    // ✅ Tạo metadata cho chat mới
    fun createChatMetadata(userId: String, chatId: String, firstMessage: String) {
        val data = hashMapOf(
            "title" to firstMessage.take(30), // lấy 30 ký tự đầu làm title
            "createdAt" to System.currentTimeMillis(),
            "lastMessage" to firstMessage
        )
        chatRef(userId, chatId).set(data)
    }

    // ✅ Cập nhật lastMessage mỗi lần có tin mới
    fun updateLastMessage(userId: String, chatId: String, lastMessage: String) {
        chatRef(userId, chatId).update("lastMessage", lastMessage)
    }

    // ✅ Load danh sách chat
    fun loadChatList(
        userId: String,
        onResult: (List<Chat>) -> Unit
    ) {
        db.collection("users")
            .document(userId)
            .collection("chats")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val chats = result.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    Chat(id = doc.id, title = title, lastMessage = lastMessage, createdAt = createdAt)
                }
                onResult(chats)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
}