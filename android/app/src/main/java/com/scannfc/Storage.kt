package com.scannfc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Thẻ đã lưu vào danh sách. */
data class SavedCard(
    val uid: String,
    val name: String,
    val type: String,
    val block0Hex: String?, // 32 ký tự hex (block 0) hoặc null
    val savedAt: Long
)

/** Một dòng nhật ký Vào/Ra. */
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
    private val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    // ---------------- Danh sách thẻ ----------------
    fun getCards(): List<SavedCard> {
        val raw = prefs.getString("cards", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SavedCard(
                    uid = o.getString("uid"),
                    name = o.getString("name"),
                    type = o.optString("type"),
                    block0Hex = o.optString("b0").ifEmpty { null },
                    savedAt = o.optLong("at")
                )
            }.sortedByDescending { it.savedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getCard(uid: String): SavedCard? = getCards().find { it.uid == uid }

    fun nameFor(uid: String): String? = getCard(uid)?.name

    fun saveCard(card: SavedCard) {
        val list = getCards().filter { it.uid != card.uid }.toMutableList()
        list.add(card)
        persistCards(list)
    }

    fun deleteCard(uid: String) = persistCards(getCards().filter { it.uid != uid })

    private fun persistCards(list: List<SavedCard>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("uid", it.uid)
                put("name", it.name)
                put("type", it.type)
                put("b0", it.block0Hex ?: "")
                put("at", it.savedAt)
            })
        }
        prefs.edit().putString("cards", arr.toString()).apply()
    }

    // ---------------- Nhật ký Vào/Ra ----------------
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

    /** Ghi 1 lần chấm công; tự xác định IN/OUT xen kẽ theo số lần của UID trong ngày. */
    fun addAttendance(uid: String, type: String): LogEntry {
        val logs = getLogs().toMutableList()
        val todayCount = logs.count { it.uid == uid && sameDay(it.time) }
        val dir = if (todayCount % 2 == 0) "IN" else "OUT"
        val entry = LogEntry(uid, nameFor(uid), type, System.currentTimeMillis(), dir)
        logs.add(0, entry)
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

    private fun sameDay(millis: Long): Boolean =
        dayFmt.format(Date(millis)) == dayFmt.format(Date())
}
