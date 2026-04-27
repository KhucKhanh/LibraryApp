package com.example.libraryapp.ai

import com.example.libraryapp.ai.AIIntentDetector.Intent
import com.example.libraryapp.ai.usecase.*

object AIPromptBuilder {

    suspend fun build(userMessage: String): String {

        if (AIContextManager.currentScreen == "ChapterReader") {
            return AskChapterUseCase.buildPrompt(userMessage)
        }

        val intent = AIIntentDetector.detect(userMessage)
        return when (intent) {
            Intent.SUMMARIZE_BOOK  -> SummarizeBookUseCase.buildPrompt(userMessage)
            Intent.ASK_CHAPTER     -> AskChapterUseCase.buildPrompt(userMessage)
            Intent.RECOMMEND_BOOKS -> RecommendBooksUseCase.buildPrompt(userMessage)
            Intent.GENERAL_CHAT    -> GeneralChatUseCase.buildPrompt(userMessage)
        }
    }
}