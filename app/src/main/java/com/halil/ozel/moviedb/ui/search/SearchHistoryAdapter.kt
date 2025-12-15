package com.halil.ozel.moviedb.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.halil.ozel.moviedb.R

class SearchHistoryAdapter(
    private var historyList: MutableList<String>,
    private val onItemClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHistory: TextView = view.findViewById(R.id.tv_history_text) // Pastikan ID ini ada di layout row
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete_history) // Ikon silang/hapus
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Kita pakai layout sederhana saja atau buat row_search_history.xml
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_keyword, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val query = historyList[position]
        holder.tvHistory.text = query

        // Klik biasa untuk mencari
        holder.itemView.setOnClickListener { onItemClick(query) }

        // Klik lama untuk menghapus (Sesuai request)
        holder.itemView.setOnLongClickListener {
            onDeleteClick(query)
            true
        }

        // Atau klik ikon silang jika Anda menambahkannya di layout
        holder.btnDelete.setOnClickListener { onDeleteClick(query) }
    }

    override fun getItemCount(): Int = historyList.size

    fun updateData(newData: List<String>) {
        historyList.clear()
        historyList.addAll(newData)
        notifyDataSetChanged()
    }
}