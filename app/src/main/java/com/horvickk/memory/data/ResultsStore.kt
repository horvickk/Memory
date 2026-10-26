package com.horvickk.memory.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.edit
import org.json.JSONArray

data class ResultEntry(val time: Long, val score: Int)

/** История результатов по упражнениям. Хранится в SharedPreferences как JSON. */
class ResultsStore(context: Context) {
    private val prefs = context.getSharedPreferences("results", Context.MODE_PRIVATE)
    private val results = mutableStateMapOf<String, List<ResultEntry>>()

    init {
        for ((key, value) in prefs.all) {
            if (value is String) results[key] = decode(value)
        }
    }

    /** Результаты от старых к новым. */
    fun of(exerciseId: String): List<ResultEntry> = results[exerciseId].orEmpty()

    fun add(exerciseId: String, score: Int, time: Long = System.currentTimeMillis()) {
        val updated = (of(exerciseId) + ResultEntry(time, score)).takeLast(MAX_ENTRIES)
        results[exerciseId] = updated
        prefs.edit { putString(exerciseId, encode(updated)) }
    }

    fun clear(exerciseId: String) {
        results.remove(exerciseId)
        prefs.edit { remove(exerciseId) }
    }

    fun clearAll() {
        results.clear()
        prefs.edit { clear() }
    }

    private fun encode(list: List<ResultEntry>): String {
        val array = JSONArray()
        for (entry in list) array.put(JSONArray().put(entry.time).put(entry.score))
        return array.toString()
    }

    private fun decode(raw: String): List<ResultEntry> = runCatching {
        val array = JSONArray(raw)
        List(array.length()) { i ->
            val item = array.getJSONArray(i)
            ResultEntry(item.getLong(0), item.getInt(1))
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val MAX_ENTRIES = 300
    }
}
