package com.example.libraryapp.ai

import com.example.libraryapp.model.Book

object AIContextManager {

    var currentScreen: String = "Home"
    var currentBook: Book? = null
    var currentChapter: String? = null
    var currentChapterContent: String? = null
    var allBooks: List<Book> = emptyList()
    var lastSelectedBook: Book? = null

    data class Snapshot(
        val screen: String,
        val book: Book?,
        val chapter: String?,
        val chapterContent: String?
    )

    fun snapshot(): Snapshot = Snapshot(
        screen = currentScreen,
        book = currentBook,
        chapter = currentChapter,
        chapterContent = currentChapterContent
    )
}