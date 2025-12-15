package com.halil.ozel.moviedb.data.models;

import com.google.gson.annotations.SerializedName;

public class VideoResult {

    @SerializedName("id")
    private String id;

    @SerializedName("iso_639_1")
    private String iso6391;

    @SerializedName("iso_3166_1")
    private String iso31661;

    @SerializedName("key")
    private String key; // Ini adalah ID video YouTube (misal: "dQw4w9WgXcQ")

    @SerializedName("name")
    private String name;

    @SerializedName("site")
    private String site; // Kita butuh ini untuk memfilter "YouTube"

    @SerializedName("size")
    private int size;

    @SerializedName("type")
    private String type; // Kita butuh ini untuk memfilter "Trailer"

    // Getters
    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getSite() {
        return site;
    }

    public String getType() {
        return type;
    }
}