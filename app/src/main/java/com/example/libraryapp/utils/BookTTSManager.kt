package com.example.libraryapp.utils

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class BookTTSManager(context: Context, private val onReady: () -> Unit = {}) {

    private var tts: TextToSpeech? = null
    var isReady = false
    private var isStopping = false

    private var pendingText: String? = null
    private var pendingCallback: (() -> Unit)? = null

    // ✅ Lưu vị trí dừng
    private var fullText: String = ""
    private var resumeIndex: Int = 0

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale("vi"))
                isReady = true
                onReady()
                pendingText?.let { text ->
                    pendingCallback?.let { cb ->
                        read(text, cb)
                        pendingText = null
                        pendingCallback = null
                    }
                }
            }
        }
    }

    fun read(text: String, onFinished: () -> Unit) {
        if (!isReady) {
            pendingText = text
            pendingCallback = onFinished
            return
        }

        // ✅ Nếu text mới khác text cũ → reset vị trí (chương mới)
        if (text != fullText) {
            fullText = text
            resumeIndex = 0
        }

        isStopping = false

        // ✅ Chia thành các câu để track vị trí
        val sentences = splitIntoSentences(fullText)
        readFromIndex(sentences, resumeIndex, onFinished)
    }

    private fun readFromIndex(
        sentences: List<String>,
        startIndex: Int,
        onFinished: () -> Unit
    ) {
        if (startIndex >= sentences.size) {
            resumeIndex = 0  // hết chương → reset
            onFinished()
            return
        }

        resumeIndex = startIndex
        val chunk = sentences[startIndex]

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (isStopping) return
                // ✅ Xong 1 câu → đọc câu tiếp
                readFromIndex(sentences, startIndex + 1, onFinished)
            }
            override fun onError(id: String?) {
                if (!isStopping) onFinished()
            }
        })

        tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, Bundle(), "tts_$startIndex")
    }

    private fun splitIntoSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.!?…\n])\\s*"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun stop() {
        isStopping = true
        pendingText = null
        pendingCallback = null
        tts?.stop()
    }

    fun resetPosition() {
        resumeIndex = 0
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}