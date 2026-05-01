package com.example.libraryapp.ui.friend

import com.example.libraryapp.ui.friend.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FriendRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val myId get() = auth.currentUser!!.uid

    // ── Danh sách bạn bè (realtime) ──────────────────────────────────────────

    fun getFriendsFlow(): Flow<List<User>> = callbackFlow {
        val ref = db.collection("users/$myId/friends")
        val sub = ref.addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val friendIds = snap.documents.map { it.id }
            if (friendIds.isEmpty()) { trySend(emptyList()); return@addSnapshotListener }

            // Fetch từng user document song song
            db.collection("users")
                .whereIn("__name__", friendIds)
                .get()
                .addOnSuccessListener { users ->
                    val list = users.documents.map { doc ->
                        User(
                            uid = doc.id,
                            displayName = doc.getString("displayName") ?: "",
                            email = doc.getString("email") ?: "",
                            avatarUrl = doc.getString("avatarUrl") ?: ""
                        )
                    }
                    trySend(list)
                }
        }
        awaitClose { sub.remove() }
    }

    // ── Lời mời kết bạn đến (realtime) ───────────────────────────────────────

    fun getFriendRequestsFlow(): Flow<List<User>> = callbackFlow {
        val ref = db.collection("users/$myId/friendRequests")
        val sub = ref.addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val fromIds = snap.documents.map { it.id }
            if (fromIds.isEmpty()) { trySend(emptyList()); return@addSnapshotListener }

            db.collection("users")
                .whereIn("__name__", fromIds)
                .get()
                .addOnSuccessListener { users ->
                    val list = users.documents.map { doc ->
                        User(
                            uid = doc.id,
                            displayName = doc.getString("displayName") ?: "",
                            email = doc.getString("email") ?: "",
                            avatarUrl = doc.getString("avatarUrl") ?: ""
                        )
                    }
                    trySend(list)
                }
        }
        awaitClose { sub.remove() }
    }

    // ── Tìm user theo email ───────────────────────────────────────────────────

    suspend fun searchUserByEmail(email: String): User? {
        val snap = db.collection("users")
            .whereEqualTo("email", email.trim().lowercase())
            .limit(1)
            .get()
            .await()
        val doc = snap.documents.firstOrNull() ?: return null
        if (doc.id == myId) return null          // không hiện chính mình
        return User(
            uid = doc.id,
            displayName = doc.getString("displayName") ?: "",
            email = doc.getString("email") ?: "",
            avatarUrl = doc.getString("avatarUrl") ?: ""
        )
    }

    // ── Kiểm tra trạng thái quan hệ ──────────────────────────────────────────

    suspend fun getRelationStatus(targetId: String): RelationStatus {
        val isFriend = db.document("users/$myId/friends/$targetId").get().await().exists()
        if (isFriend) return RelationStatus.FRIEND

        val sentReq = db.document("users/$targetId/friendRequests/$myId").get().await().exists()
        if (sentReq) return RelationStatus.REQUEST_SENT

        val receivedReq = db.document("users/$myId/friendRequests/$targetId").get().await().exists()
        if (receivedReq) return RelationStatus.REQUEST_RECEIVED

        return RelationStatus.NONE
    }

    // ── Gửi lời mời kết bạn ──────────────────────────────────────────────────

    suspend fun sendFriendRequest(toId: String) {
        db.document("users/$toId/friendRequests/$myId")
            .set(mapOf("fromId" to myId, "status" to "pending"))
            .await()
    }

    // ── Chấp nhận lời mời ────────────────────────────────────────────────────

    suspend fun acceptFriendRequest(fromId: String) {
        val batch = db.batch()
        batch.set(db.document("users/$myId/friends/$fromId"), mapOf("since" to com.google.firebase.Timestamp.now()))
        batch.set(db.document("users/$fromId/friends/$myId"), mapOf("since" to com.google.firebase.Timestamp.now()))
        batch.delete(db.document("users/$myId/friendRequests/$fromId"))
        batch.commit().await()
    }

    // ── Từ chối / hủy lời mời ────────────────────────────────────────────────

    suspend fun declineFriendRequest(fromId: String) {
        db.document("users/$myId/friendRequests/$fromId").delete().await()
    }

    suspend fun cancelFriendRequest(toId: String) {
        db.document("users/$toId/friendRequests/$myId").delete().await()
    }

    // ── Hủy kết bạn ──────────────────────────────────────────────────────────

    suspend fun removeFriend(friendId: String) {
        val batch = db.batch()
        batch.delete(db.document("users/$myId/friends/$friendId"))
        batch.delete(db.document("users/$friendId/friends/$myId"))
        batch.commit().await()
    }
}

enum class RelationStatus { NONE, FRIEND, REQUEST_SENT, REQUEST_RECEIVED }