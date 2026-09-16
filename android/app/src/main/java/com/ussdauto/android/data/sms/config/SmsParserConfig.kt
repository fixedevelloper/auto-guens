package com.ussdauto.android.data.sms.config

import android.content.Context
import com.ussdauto.android.R
import com.ussdauto.android.domain.model.OperationType
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class OperatorSmsPatterns(
    val senders: List<String>,
    val operationKeywords: Map<OperationType, Regex>,
    val successPattern: Regex,
    val failurePattern: Regex?
)

/**
 * Charge les regex de reconnaissance des SMS de confirmation depuis un fichier de
 * config externalisé (res/raw/sms_parser_patterns.json), plutôt que de les hardcoder
 * dans le code des parseurs — un changement de libellé opérateur ne nécessite alors
 * pas de recompilation, seulement une mise à jour de ce fichier.
 */
@Singleton
class SmsParserConfig @Inject constructor(
    @ApplicationContext context: Context
) {
    private val patternsByOperateur: Map<String, OperatorSmsPatterns> by lazy {
        val json = context.resources.openRawResource(R.raw.sms_parser_patterns)
            .bufferedReader()
            .use { it.readText() }
        parse(json)
    }

    fun forOperateur(key: String): OperatorSmsPatterns? = patternsByOperateur[key]

    private fun parse(json: String): Map<String, OperatorSmsPatterns> {
        val root = JSONObject(json)
        val result = mutableMapOf<String, OperatorSmsPatterns>()

        root.keys().forEach { operateurKey ->
            try {
                val node = root.getJSONObject(operateurKey)

                val senders = node.getJSONArray("senders").let { array ->
                    (0 until array.length()).map { array.getString(it) }
                }

                val keywordsNode = node.getJSONObject("operationKeywords")
                val operationKeywords = OperationType.entries.mapNotNull { type ->
                    keywordsNode.optString(type.name, null.toString()).takeIf { it.isNotBlank() && it != "null" }
                        ?.let { type to Regex(it, RegexOption.IGNORE_CASE) }
                }.toMap()

                val successPattern = Regex(node.getString("successPattern"))
                val failurePattern = node.optString("failurePattern", "").takeIf { it.isNotBlank() }?.let(::Regex)

                result[operateurKey] = OperatorSmsPatterns(senders, operationKeywords, successPattern, failurePattern)
            } catch (e: Exception) {
                Timber.e(e, "Config SMS invalide pour l'opérateur %s", operateurKey)
            }
        }

        return result
    }
}
