package com.ringerguard.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestampMs: Long,
    val title: String,
    val detail: String,
    val type: String,
)

object EventLogger {
    private const val MAX_ENTRIES = 200
    private const val FILE_NAME = "event_log.json"

    fun log(context: android.content.Context, title: String, detail: String, type: String) {
        val entries = getAll(context).toMutableList()
        entries.add(0, LogEntry(System.currentTimeMillis(), title, detail, type))
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(entries.lastIndex)
        }
        persist(context, entries)
    }

    fun getAll(context: android.content.Context): List<LogEntry> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        LogEntry(
                            timestampMs = obj.getLong("timestampMs"),
                            title = obj.getString("title"),
                            detail = obj.getString("detail"),
                            type = obj.getString("type"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun clear(context: android.content.Context) {
        File(context.filesDir, FILE_NAME).delete()
    }

    fun sevenDaySummary(context: android.content.Context): String {
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val recent = getAll(context).filter { it.timestampMs >= cutoff }
        val restored = recent.count { it.type == "restored" }
        val silenced = recent.count { it.type == "silenced" }
        return "Last 7 days: $restored auto-restores, $silenced grace periods triggered."
    }

    fun formatTime(timestampMs: Long): String {
        return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestampMs))
    }

    private fun persist(context: android.content.Context, entries: List<LogEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("timestampMs", entry.timestampMs)
                    .put("title", entry.title)
                    .put("detail", entry.detail)
                    .put("type", entry.type),
            )
        }
        File(context.filesDir, FILE_NAME).writeText(array.toString())
    }
}
