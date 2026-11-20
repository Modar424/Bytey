package com.bytewise.game.data.source

import android.content.Context
import com.bytewise.game.data.model.AppLanguage
import com.bytewise.game.data.model.ConceptDto
import com.bytewise.game.data.model.QuestionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException

/** Reads the bundled JSON content. Returns null when a file does not exist (yet). */
class AssetContentSource(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadQuestions(language: AppLanguage): List<QuestionDto>? =
        read("questions/questions_${language.tag}.json", ListSerializer(QuestionDto.serializer()))

    suspend fun loadConcepts(language: AppLanguage): List<ConceptDto>? =
        read("concepts/concepts_${language.tag}.json", ListSerializer(ConceptDto.serializer()))

    private suspend fun <T> read(path: String, serializer: KSerializer<List<T>>): List<T>? =
        withContext(Dispatchers.IO) {
            try {
                val text = context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
                json.decodeFromString(serializer, text)
            } catch (e: FileNotFoundException) {
                null
            }
        }
}
