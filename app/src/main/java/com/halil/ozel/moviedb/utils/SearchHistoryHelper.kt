package com.halil.ozel.moviedb.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryHelper(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("SEARCH_PREF", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getHistory(): ArrayList<String> {
        val json = sharedPreferences.getString("HISTORY_LIST", null)
        val type = object : TypeToken<ArrayList<String>>() {}.type
        return if (json != null) gson.fromJson(json, type) else ArrayList()
    }

    fun addHistory(query: String) {
        val list = getHistory()
        // Hapus jika sudah ada (supaya yang baru naik ke atas)
        if (list.contains(query)) {
            list.remove(query)
        }
        list.add(0, query) // Tambah ke paling atas

        // Batasi simpan maksimal 10 riwayat agar ringan
        if (list.size > 10) {
            list.subList(10, list.size).clear()
        }
        saveList(list)
    }

    fun deleteHistory(query: String) {
        val list = getHistory()
        list.remove(query)
        saveList(list)
    }

    private fun saveList(list: ArrayList<String>) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(list)
        editor.putString("HISTORY_LIST", json)
        editor.apply()
    }
}