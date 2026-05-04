package com.example.qrscanner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val content: String,
    val timestamp: Long
)

object ScanHistoryStore {
    private const val PREFS_NAME = "scan_history"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 50

    fun addEntry(context: Context, content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return
        }
        val entries = getEntries(context).filterNot { it.content == trimmed }.toMutableList()
        entries.add(0, HistoryEntry(trimmed, System.currentTimeMillis()))
        val trimmedEntries = entries.take(MAX_ENTRIES)
        saveEntries(context, trimmedEntries)
    }

    fun getEntries(context: Context): List<HistoryEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val results = mutableListOf<HistoryEntry>()
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val content = obj.optString("content").trim()
                val timestamp = obj.optLong("timestamp", 0L)
                if (content.isNotEmpty()) {
                    results.add(HistoryEntry(content, timestamp))
                }
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveEntries(context: Context, entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("content", entry.content)
            obj.put("timestamp", entry.timestamp)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENTRIES, array.toString())
            .apply()
    }
}
