package com.example.libraryapp.ai

import com.example.libraryapp.ai.prompt.*

object ScreenPromptBuilder {

    suspend fun build(
        context: AIContextManager.Snapshot,
        userMessage: String
    ): String {
        return when (context.screen) {
            "Home" -> HomePrompt.build()

            "BookDetail" -> BookDetailPrompt.build(context)

            "ChapterReader" ->
                ChapterReaderPrompt.build(context, userMessage)

            else -> FallbackPrompt.build()
        }
    }
}