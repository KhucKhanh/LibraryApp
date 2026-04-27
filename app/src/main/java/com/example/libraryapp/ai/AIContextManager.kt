package com.example.libraryapp.ai

import com.example.libraryapp.model.Book

object AIContextManager {

    var currentScreen: String = "Home"
    var currentBook: Book? = null
    var currentChapter: String? = null
    var currentChapterContent: String? = null
    var allBooks: List<Book> = emptyList()
    var lastSelectedBook: Book? = null

}