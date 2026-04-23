package com.example.libraryapp.ui.detail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.libraryapp.data.ChapterRepository
import com.example.libraryapp.model.Chapter

class BookDetailViewModel : ViewModel() {

    private val repository = ChapterRepository()

    private val _chapters = MutableLiveData<List<Chapter>>()
    val chapters: LiveData<List<Chapter>> = _chapters

    fun loadChapters(bookId: String) {
        repository.getChaptersByBook(bookId) {

            Log.d("VIEWMODEL", "chapters loaded: ${it.size}")

            _chapters.value = it
        }
    }
}