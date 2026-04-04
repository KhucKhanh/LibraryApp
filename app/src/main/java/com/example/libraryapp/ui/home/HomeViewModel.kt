package com.example.libraryapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.libraryapp.model.Book

class HomeViewModel : ViewModel() {

    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    init {
        loadBooks()
    }

    private fun loadBooks() {
        _books.value = listOf(
            Book("book_1", "Clean Code", "Robert", "1", "..."),

        )
    }
}