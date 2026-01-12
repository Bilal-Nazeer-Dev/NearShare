package com.example.nearshare

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object HistoryRepository {

    private const val PREF_NAME = "NearShareHistory"
    private const val KEY_HISTORY = "history_list"

    fun saveItem(context: Context, item: HistoryItem) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val existingList = getHistory(context).toMutableList()
        existingList.add(0, item)
        saveToPrefs(context, existingList)
    }

    fun getHistory(context: Context): List<HistoryItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_HISTORY, "[]")
        val list = mutableListOf<HistoryItem>()

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    HistoryItem(
                        fileName = obj.optString("fileName"),
                        fileSize = obj.optString("fileSize"),
                        status = obj.optString("status"),
                        timestamp = obj.optLong("timestamp"),
                        filePath = obj.optString("filePath") // Load Path
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveToPrefs(context: Context, list: List<HistoryItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        list.forEach {
            val jsonObject = JSONObject().apply {
                put("fileName", it.fileName)
                put("fileSize", it.fileSize)
                put("status", it.status)
                put("timestamp", it.timestamp)
                put("filePath", it.filePath) // Save Path
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }
}