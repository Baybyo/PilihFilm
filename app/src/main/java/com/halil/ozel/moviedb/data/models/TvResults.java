package com.halil.ozel.moviedb.data.models;

public class TvResults {
    private Integer id;
    private String name;
    private String poster_path;

    // 🆕 Tambahan Data Baru
    private String first_air_date;
    private Double vote_average;

    // 1. Konstruktor Kosong (Wajib ada)
    public TvResults() {
    }

    // 2. Konstruktor Manual (Tetap dipertahankan agar kode lain tidak error)
    public TvResults(Integer id, String name, String poster_path) {
        this.id = id;
        this.name = name;
        this.poster_path = poster_path;
    }

    // --- GETTER & SETTER ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPoster_path() { return poster_path; }
    public void setPoster_path(String poster_path) { this.poster_path = poster_path; }

    // 🆕 Getter Setter Baru
    public String getFirst_air_date() { return first_air_date; }
    public void setFirst_air_date(String first_air_date) { this.first_air_date = first_air_date; }

    public Double getVote_average() { return vote_average; }
    public void setVote_average(Double vote_average) { this.vote_average = vote_average; }
}