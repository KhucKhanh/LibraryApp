package com.example.libraryapp.ui.reader

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.libraryapp.model.Chapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ChapterViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth = FirebaseAuth.getInstance()

    val chapters = MutableLiveData<List<Chapter>>()
    val currentChapter = MutableLiveData<Chapter>()
    private var currentIndex = 0

    private fun getUid(): String? = auth.currentUser?.uid

    // ───────────────────────────────────────────
    // LOAD CHAPTERS
    // ───────────────────────────────────────────
    fun loadChapters(bookId: String, startOrder: Int = 0) {
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

                    // 🔥 luôn lưu recent + progress (kể cả = 0)
                    saveRecentBook(bookId, currentIndex)
                    saveReadingPosition(bookId, currentIndex, 0)
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
    // SAVE PROGRESS
    // ───────────────────────────────────────────
    fun saveReadingPosition(bookId: String, order: Int, scrollY: Int) {
        val uid = getUid() ?: return

        db.collection("users")
            .document(uid)
            .collection("readingProgress")
            .document(bookId)
            .set(
                mapOf(
                    "lastChapterOrder" to order,   // 🔥 unified name
                    "chapter_${order}_scrollY" to scrollY,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
    }

    // ───────────────────────────────────────────
    // GET SCROLL
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
    // GET LAST READ (FIX -1)
    // ───────────────────────────────────────────
    fun getLastReadOrder(bookId: String, onResult: (Int) -> Unit) {
        val uid = getUid() ?: run { onResult(-1); return }

        db.collection("users")
            .document(uid)
            .collection("readingProgress")
            .document(bookId)
            .get()
            .addOnSuccessListener { doc ->
                val lastOrder = doc.getLong("lastChapterOrder")?.toInt() ?: -1
                Log.d("DEBUG_BOOK", "lastOrder = $lastOrder")
                onResult(lastOrder)
            }
            .addOnFailureListener {
                onResult(-1)
            }
    }

    // ───────────────────────────────────────────
    // RECENT BOOK (FIX)
    // ───────────────────────────────────────────
    fun saveRecentBook(bookId: String, chapterOrder: Int) {
        val uid = getUid() ?: return

        val data = hashMapOf(
            "bookId" to bookId,
            "lastChapterOrder" to chapterOrder, // 🔥 same field
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(uid)
            .collection("recentBooks")
            .document(bookId)
            .set(data)
    }

    // ───────────────────────────────────────────
    // HELPER
    // ───────────────────────────────────────────
    fun isLastChapter(): Boolean {
        val list = chapters.value ?: return false
        val current = currentChapter.value ?: return false
        return current.order == list.lastOrNull()?.order
    }

    fun isFirstChapter(): Boolean = currentIndex == 0

    fun hasNextChapter(): Boolean {
        val list = chapters.value ?: return false
        return currentIndex < list.size - 1
    }
}