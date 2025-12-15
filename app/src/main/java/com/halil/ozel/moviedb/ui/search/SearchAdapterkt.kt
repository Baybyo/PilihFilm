package com.halil.ozel.moviedb.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.halil.ozel.moviedb.databinding.ItemMovieBinding
import com.halil.ozel.moviedb.data.models.Movie
import com.halil.ozel.moviedb.data.models.Results

// PERBAIKAN: Hapus tanda kurung "()" setelah RecyclerView.Adapter<...>
// Kita akan memanggil super() di dalam constructor di bawah.
class SearchAdapterkt : RecyclerView.Adapter<SearchAdapterkt.VH> {

    private var resultList: List<Results> = ArrayList()
    private var movieList: List<Movie> = ArrayList()
    private var isMovieMode: Boolean = false

    var onItemClick: ((Results) -> Unit)? = null
    var onMovieClick: ((Movie) -> Unit)? = null

    // --- CONSTRUCTOR 1: Default (Tanpa Argumen) ---
    // Dipakai oleh sistem atau inisialisasi kosong
    constructor() : super()

    // --- CONSTRUCTOR 2: Untuk SearchFragment (Results) ---
    // Dipakai oleh Java: new SearchAdapterkt(listResults)
    constructor(items: List<Results>) : super() {
        this.resultList = items
        this.isMovieMode = false
    }

    // --- Method ViewHolder & Binding ---

    inner class VH(private val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindResult(data: Results) {
            binding.tvTitle.text = data.title ?: "-"
            val posterUrl = data.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
            loadPoster(posterUrl)
            binding.root.setOnClickListener { onItemClick?.invoke(data) }
        }

        fun bindMovie(data: Movie) {
            binding.tvTitle.text = data.title ?: "-"
            val posterUrl = data.posterUrl?.let { "https://image.tmdb.org/t/p/w500$it" }
            loadPoster(posterUrl)
            binding.root.setOnClickListener { onMovieClick?.invoke(data) }
        }

        private fun loadPoster(url: String?) {
            Glide.with(binding.root.context)
                .load(url)
                .into(binding.imgPoster)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        if (isMovieMode) {
            holder.bindMovie(movieList[position])
        } else {
            holder.bindResult(resultList[position])
        }
    }

    override fun getItemCount(): Int {
        return if (isMovieMode) movieList.size else resultList.size
    }

    // --- Helper Methods untuk Update Data ---

    // Dipakai oleh SearchFragment.java
    fun updateList(newItems: List<Results>) {
        this.resultList = newItems
        this.isMovieMode = false
        notifyDataSetChanged()
    }

    // Dipakai oleh SearchActivity.kt
    fun updateData(newItems: List<Movie>) {
        this.movieList = newItems
        this.isMovieMode = true
        notifyDataSetChanged()
    }
}