package com.example.libraryapp.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.libraryapp.model.Chapter
import com.google.firebase.firestore.FirebaseFirestore

class ChapterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()

    val chapters = MutableLiveData<List<Chapter>>()
    val currentChapter = MutableLiveData<Chapter>()
    private var currentIndex = 0

    // Load chapters theo bookId và order bắt đầu
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

    fun saveReadingPosition(bookId: String, order: Int, scrollY: Int) {
        val prefs = getApplication<Application>().getSharedPreferences("reading_pos", 0)
        prefs.edit()
            .putInt("${bookId}_order", order)
            .putInt("${bookId}_chapter_${order}_scrollY", scrollY) // ✅ key theo chapter
            .apply()
    }

    fun getScrollForChapter(bookId: String, order: Int): Int {
        val prefs = getApplication<Application>().getSharedPreferences("reading_pos", 0)
        return prefs.getInt("${bookId}_chapter_${order}_scrollY", 0) // ✅ lấy đúng chapter
    }

    fun getLastReadOrder(bookId: String): Int {
        val prefs = getApplication<Application>().getSharedPreferences("reading_pos", 0)
        return prefs.getInt("${bookId}_order", 1)
    }
}