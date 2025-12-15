package com.halil.ozel.moviedb.data.models

// Kelas utama yang menampung daftar film dari respons API
data class MovieResponse(
    // Properti ini sudah benar dan berasal dari kode utama Anda
    val page: Int,

    // Menggunakan Movie, bukan List<MovieRespon>
    val results: List<Movie>,

    val total_results: Int,
    val total_pages: Int
)

// Kelas yang merepresentasikan satu objek film (Diubah namanya dari MovieRespon)
data class MovieRespon(
    val id: Int,
    val title: String?,
    val overview: String?,
    val poster_path: String?,
    val release_date: String?

    // Catatan: Jika Anda menggunakan retrofit dan serialisasi Gson/Moshi,
    // val poster_path: String? secara otomatis memetakan ke JSON key "poster_path"
    // (sama seperti @SerializedName("poster_path") di Java)
)