package com.halil.ozel.moviedb.data.models;
import com.google.gson.annotations.SerializedName;
public class Movie {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("backdrop_path")
    private String backdropPath;

    @SerializedName("vote_average")
    private double rating;

    @SerializedName("overview")
    private String overview;

    @SerializedName("release_date")
    private String releaseDate;

    // Getter
    public int getId() { return id; }
    public String getTitle() { return title; }

    // Trik: TMDB hanya memberi nama file, kita harus tambah URL depannya
    public String getPosterUrl() {
        return "https://image.tmdb.org/t/p/w500" + posterPath;
    }

    public String getBackdropUrl() {
        return "https://image.tmdb.org/t/p/w780" + backdropPath;
    }

    public String getRating() { return String.valueOf(rating); }
    public String getOverview() { return overview; }
    public String getReleaseDate() { return releaseDate; }
}