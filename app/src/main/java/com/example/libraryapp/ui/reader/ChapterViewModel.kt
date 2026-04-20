package com.example.libraryapp.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.libraryapp.model.Chapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ChapterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val chapters = MutableLiveData<List<Chapter>>()
    val currentChapter = MutableLiveData<Chapter>()
    private var currentIndex = 0

    private fun getUid(): String? = auth.currentUser?.uid

    // ───────────────────────────────────────────
    // Load chapters + nhảy đúng chapter
    // ───────────────────────────────────────────
    fun loadChapters(bookId: String, startOrder: Int = 1) {
        db.collection("books")
            .document(bookId)
            .collection("chapters")
            .orderBy("order")
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Chapter::class.java)
                chapters.value = list

                if (list.isNotEmpty()) {
                    val index = list.indexOfFirst { it.order == startOrder }
                    currentIndex = if (index != -1) index else 0
                    currentChapter.value = list[currentIndex]
                }
            }
    }

    fun nextChapter() {
        val list = chapters.value ?: return
        if (currentIndex < list.size - 1) {
            currentIndex++
            currentChapter.value = list[currentIndex]
        }
    }

    fun prevChapter() {
        val list = chapters.value ?: return
        if (currentIndex > 0) {
            currentIndex--
            currentChapter.value = list[currentIndex]
        }
    }

    // ───────────────────────────────────────────
    // Lưu vị trí đọc lên Firestore
    // ───────────────────────────────────────────
    fun saveReadingPosition(bookId: String, order: Int, scrollY: Int) {
        val uid = getUid() ?: return

        db.collection("users")
            .document(uid)
            .collection("readingProgress")
            .document(bookId)
            .set(
                mapOf(
                    "lastOrder" to order,
                    "chapter_${order}_scrollY" to scrollY,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
    }

    // ───────────────────────────────────────────
    // Lấy scroll position của chapter (từ Firestore, callback)
    // ───────────────────────────────────────────
    fun getScrollForChapter(bookId: String, order: Int, onResult: (Int) -> Unit) {
        val uid = getUid() ?: run { onResult(0); return }

        db.collection("users")
            .document(uid)
            .collection("readingProgress")
            .document(bookId)
            .get()
            .addOnSuccessListener { doc ->
                val scrollY = doc.getLong("chapter_${order}_scrollY")?.toInt() ?: 0
                onResult(scrollY)
            }
            .addOnFailureListener {
                onResult(0)
            }
    }

    // ───────────────────────────────────────────
    // Lấy chapter cuối cùng đã đọc (từ Firestore, callback)
    // ───────────────────────────────────────────
    fun getLastReadOrder(bookId: String, onResult: (Int) -> Unit) {
        val uid = getUid() ?: run { onResult(1); return }

        db.collection("users")
            .document(uid)
            .collection("readingProgress")
            .document(bookId)
            .get()
            .addOnSuccessListener { doc ->
                val lastOrder = doc.getLong("lastOrder")?.toInt() ?: 1
                onResult(lastOrder)
            }
            .addOnFailureListener {
                onResult(1)
            }
    }

    fun isLastChapter(): Boolean {
        val list = chapters.value ?: return false
        val current = currentChapter.value ?: return false

        return current.order == list.lastOrNull()?.order
    }

}