package com.scannfc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Một dòng nhật ký quẹt thẻ. */
data class LogEntry(
    val uid: String,
    val name: String?,
    val type: String,
    val time: Long,
    val direction: String // "IN" hoặc "OUT"
) {
    fun timeText(): String =
        SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(time))
}

/** Lưu trữ nhẹ bằng SharedPreferences (không dùng DB). */
class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("scannfc", Context.MODE_PRIVATE)

    // --- Tên thẻ theo UID ---
    fun getName(uid: String): String? = prefs.getString("name_$uid", null)

    fun setName(uid: String, name: String) {
        val v = name.trim()
        prefs.edit().apply {
            if (v.isEmpty()) remove("name_$uid") else putString("name_$uid", v)
        }.apply()
    }

    // --- Lịch sử ra/vào ---
    fun getLogs(): List<LogEntry> {
        val raw = prefs.getString("logs", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                LogEntry(
                    uid = o.getString("uid"),
                    name = o.optString("name").ifEmpty { null },
                    type = o.optString("type"),
                    time = o.getLong("time"),
                    direction = o.getString("dir")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Thêm 1 lần quẹt; tự xác định IN/OUT xen kẽ theo số lần của UID trong ngày. */
    fun addScan(card: CardInfo): LogEntry {
        val logs = getLogs().toMutableList()
        val todayCount = logs.count { it.uid == card.uid && sameDay(it.time) }
        val dir = if (todayCount % 2 == 0) "IN" else "OUT"
        val entry = LogEntry(card.uid, getName(card.uid), card.type, System.currentTimeMillis(), dir)
        logs.add(0, entry) // mới nhất lên đầu
        saveLogs(logs.take(300))
        return entry
    }

    fun clearLogs() = prefs.edit().remove("logs").apply()

    private fun saveLogs(logs: List<LogEntry>) {
        val arr = JSONArray()
        logs.forEach {
            arr.put(JSONObject().apply {
                put("uid", it.uid)
                put("name", it.name ?: "")
                put("type", it.type)
                put("time", it.time)
                put("dir", it.direction)
            })
        }
        prefs.edit().putString("logs", arr.toString()).apply()
    }

    private val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private fun sameDay(millis: Long): Boolean =
        dayFmt.format(Date(millis)) == dayFmt.format(Date())
}
