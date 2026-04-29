package com.example.libraryapp.ui.home

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.libraryapp.ai.AIContextManager
import com.example.libraryapp.model.Book
import com.example.libraryapp.recommendation.HybridRecommendationEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    val books = MutableLiveData<List<Book>>()
    val recentBooks = MutableLiveData<List<Book>>()
    val recommendedBooks = MutableLiveData<List<Book>>()

    init {
        loadBooks()
    }

    private fun loadBooks() {
        // Cache trước → hiện ngay
        db.collection("books")
            .get(com.google.firebase.firestore.Source.CACHE)
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val list = result.documents.mapNotNull { doc ->
                        doc.toObject(Book::class.java)?.copy(id = doc.id)
                    }
                    books.value = list
                    AIContextManager.allBooks = list
                }
            }
            .addOnCompleteListener {
                // Server update ngầm
                db.collection("books")
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener { result ->
                        val list = result.documents.mapNotNull { doc ->
                            doc.toObject(Book::class.java)?.copy(id = doc.id)
                        }
                        books.value = list
                        AIContextManager.allBooks = list
                    }
                    .addOnFailureListener { e ->
                        Log.e("HOME_DEBUG", "Server fetch failed: ${e.message}")
                    }
            }
    }

    fun loadRecentBooks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("recentBooks")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val bookIds = result.map { it.id }

                if (bookIds.isEmpty()) {
                    recentBooks.value = emptyList()
                    return@addOnSuccessListener
                }

                db.collection("books")
                    .whereIn(FieldPath.documentId(), bookIds)
                    .get()
                    .addOnSuccessListener { booksResult ->
                        val booksMap = booksResult.documents.associateBy { it.id }

                        val sortedBooks = bookIds.mapNotNull { id ->
                            booksMap[id]?.toObject(Book::class.java)?.copy(id = id)
                        }

                        recentBooks.value = sortedBooks
                    }
            }
    }

    fun loadRecommendations() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->

                val allData = doc.data ?: emptyMap()
                val rawScoreMap = allData
                    .filterKeys { it.startsWith("categoryScore.") }
                    .mapKeys { it.key.removePrefix("categoryScore.") }
                    .mapValues { (it.value as? Long) ?: 0L }
                Log.d("RECOMMEND_SCORE", "rawScoreMap = $rawScoreMap")

                // Normalize + lọc key rác (test, ko co, v.v.)
                val scoreMap = rawScoreMap
                    .mapKeys { it.key.trim().lowercase() }
                    .filter { entry ->
                        entry.key.isNotBlank() &&
                                !entry.key.startsWith("test") &&
                                entry.key != "ko co" &&
                                entry.key != "ko có"
                    }
                Log.d("RECOMMEND_SCORE", "userId = $userId")
                Log.d("RECOMMEND_SCORE", "doc exists = ${doc.exists()}")
                Log.d("RECOMMEND_SCORE", "doc data = ${doc.data}")
                Log.d("RECOMMEND_SCORE", "scoreMap sau normalize = $scoreMap")


                // Lấy danh sách sách đã đọc
                db.collection("users").document(userId)
                    .collection("recentBooks")
                    .limit(20)
                    .get()
                    .addOnSuccessListener { recentResult ->

                        val readBookIds = recentResult.documents.map { it.id }.toSet()

                        // Lấy toàn bộ sách
                        db.collection("books").get()
                            .addOnSuccessListener { allResult ->

                                val allBooks = allResult.documents.mapNotNull { d ->
                                    d.toObject(Book::class.java)?.copy(id = d.id)
                                }

                                // Sách chưa đọc
                                val unreadBooks = allBooks.filter { it.id !in readBookIds }

                                // Object sách đã đọc → dùng cho content-based (author + tags)
                                val recentBookObjects = allBooks.filter { it.id in readBookIds }

                                if (scoreMap.isEmpty()) {
                                    // Chưa có history → random sách chưa đọc
                                    recommendedBooks.value = unreadBooks.shuffled().take(10)
                                    return@addOnSuccessListener
                                }

                                // 🔥 Hybrid rank
                                val ranked = HybridRecommendationEngine.rank(
                                    books = unreadBooks,
                                    scoreMap = scoreMap,
                                    recentBooks = recentBookObjects
                                )

                                Log.d("RECOMMEND_DEBUG", "scoreMap = $scoreMap")
                                Log.d("RECOMMEND_DEBUG", "readBookIds = $readBookIds")
                                Log.d("RECOMMEND_DEBUG", "unreadBooks = ${unreadBooks.size}")
                                Log.d("RECOMMEND_DEBUG", "ranked = ${ranked.map { it.title }}")

                                // Nếu không đủ sách chưa đọc → bổ sung sách đã đọc vào cuối
                                val result = if (ranked.size >= 10) {
                                    ranked.take(10)
                                } else {
                                    val extra = allBooks
                                        .filter { it.id in readBookIds }
                                        .shuffled()
                                        .take(10 - ranked.size)
                                    ranked + extra
                                }

                                recommendedBooks.value = result
                            }
                    }
            }
    }
}