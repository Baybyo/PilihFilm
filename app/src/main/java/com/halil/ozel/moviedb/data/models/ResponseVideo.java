package com.halil.ozel.moviedb.data.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ResponseVideo {

    @SerializedName("id")
    private int id;

    @SerializedName("results")
    private List<VideoResult> results; // List dari class VideoResult di atas

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<VideoResult> getResults() {
        return results;
    }

    public void setResults(List<VideoResult> results) {
        this.results = results;
    }
}