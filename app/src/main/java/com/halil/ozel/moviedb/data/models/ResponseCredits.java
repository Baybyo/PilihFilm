package com.halil.ozel.moviedb.data.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ResponseCredits {

    @SerializedName("id")
    private int id;

    @SerializedName("cast")
    private List<Cast> cast;

    @SerializedName("crew")
    private List<Object> crew; // Kita gunakan Object atau List kosong jika tidak butuh data Crew

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Cast> getCast() {
        return cast;
    }

    public void setCast(List<Cast> cast) {
        this.cast = cast;
    }
}