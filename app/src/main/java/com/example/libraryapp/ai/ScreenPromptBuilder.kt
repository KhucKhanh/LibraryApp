package com.example.libraryapp.ai

import android.util.Log
import com.example.libraryapp.ai.prompt.*

object ScreenPromptBuilder {


    suspend fun build(
        context: AIContextManager.Snapshot,
        userMessage: String
    ): String {
        val prompt = when (context.screen) {
            "Home" ->
                HomePrompt.build(userMessage)

            "BookDetail" ->
                BookDetailPrompt.build(context, userMessage)

            "ChapterReader" ->
                ChapterReaderPrompt.build(context, userMessage)

            else ->
                FallbackPrompt.build(userMessage)
        }

        Log.d("PROMPT_DEBUG", "Screen: ${context.screen} | Prompt length: ${prompt.length} chars")

        return prompt
    }
}