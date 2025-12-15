package com.halil.ozel.moviedb.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.halil.ozel.moviedb.data.Api.ApiClient
import com.halil.ozel.moviedb.data.models.MovieResponse
import com.halil.ozel.moviedb.databinding.ActivitySearchBinding
import com.halil.ozel.moviedb.utils.SearchHistoryHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var searchAdapterkt: SearchAdapterkt // Adapter Hasil Film
    private lateinit var historyAdapter: SearchHistoryAdapter // Adapter Riwayat
    private lateinit var historyHelper: SearchHistoryHelper

    // Masukkan API Key Anda
    private val apiKey = "2ed365d4f123f11a6b37e764168fe81a"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        historyHelper = SearchHistoryHelper(this)

        setupToolbar()
        setupResultsRecycler()
        setupHistoryRecycler()
        setupSearchView()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupResultsRecycler() {
        searchAdapterkt = SearchAdapterkt(emptyList())
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapterkt

        searchAdapterkt.onItemClick = { movie ->
            val intent = Intent(this, com.halil.ozel.moviedb.ui.detail.MovieDetailActivity::class.java)
            intent.putExtra("movie_id", movie.id)
            startActivity(intent)
        }
    }

    private fun setupHistoryRecycler() {
        historyAdapter = SearchHistoryAdapter(
            historyHelper.getHistory(),
            onItemClick = { query ->
                // Klik riwayat -> Langsung cari
                binding.searchView.setQuery(query, true)
            },
            onDeleteClick = { query ->
                // Tahan lama -> Hapus
                showDeleteDialog(query)
            }
        )
        binding.rvSearchHistory.layoutManager = LinearLayoutManager(this)
        binding.rvSearchHistory.adapter = historyAdapter
    }

    private fun setupSearchView() {
        // Fokus -> Tampilkan riwayat
        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showHistoryUI()
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // ENTER DITEKAN -> BARU CARI KE API
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    binding.searchView.clearFocus() // Tutup keyboard
                    historyHelper.addHistory(query) // Simpan riwayat
                    performSearch(query) // Panggil API
                }
                return true
            }

            // MENGETIK -> FILTER RIWAYAT LOKAL (TIDAK PANGGIL API)
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    historyAdapter.updateData(historyHelper.getHistory())
                    showHistoryUI()
                } else {
                    // Filter list riwayat yang cocok dengan ketikan
                    val filtered = historyHelper.getHistory().filter {
                        it.contains(newText, ignoreCase = true)
                    }
                    historyAdapter.updateData(filtered)
                    showHistoryUI()
                }
                return true
            }
        })
    }

    private fun showHistoryUI() {
        binding.rvSearchResults.visibility = View.GONE
        binding.rvSearchHistory.visibility = View.VISIBLE
    }

    private fun showResultsUI() {
        binding.rvSearchHistory.visibility = View.GONE
        binding.rvSearchResults.visibility = View.VISIBLE
    }

    private fun showDeleteDialog(query: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Riwayat")
            .setMessage("Hapus '$query' dari riwayat?")
            .setPositiveButton("Ya") { _, _ ->
                historyHelper.deleteHistory(query)
                // Refresh list
                val currentQuery = binding.searchView.query.toString()
                if (currentQuery.isEmpty()) {
                    historyAdapter.updateData(historyHelper.getHistory())
                } else {
                    val filtered = historyHelper.getHistory().filter { it.contains(currentQuery, ignoreCase = true) }
                    historyAdapter.updateData(filtered)
                }
                Toast.makeText(this, "Dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performSearch(query: String) {
        showResultsUI()
        binding.progressBar.visibility = View.VISIBLE

        ApiClient.service.searchMovies(apiKey, query)
            .enqueue(object : Callback<MovieResponse> {
                override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val movies = response.body()?.results ?: emptyList()
                        if (movies.isEmpty()) {
                            Toast.makeText(this@SearchActivity, "Tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                        searchAdapterkt.updateData(movies)
                    } else {
                        Toast.makeText(this@SearchActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@SearchActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}