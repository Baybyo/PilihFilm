package com.halil.ozel.moviedb.ui.home.activity;
import android.content.Context;
import android.content.SharedPreferences;
public class preferenceHelper {
    private final SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "pilihfilm_pref";

    // Kunci untuk menyimpan data
    private static final String KEY_LANGUAGE = "language_code";
    private static final String KEY_SAFE_SEARCH = "safe_search";

    public preferenceHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // --- PENGATURAN BAHASA ---
    public void setLanguage(String langCode) {
        // simpan "id" untuk Indo, "en" untuk Inggris
        sharedPreferences.edit().putString(KEY_LANGUAGE, langCode).apply();
    }

    public String getLanguage() {
        // Default bahasa Indonesia ("id") jika belum pernah disetting
        return sharedPreferences.getString(KEY_LANGUAGE, "id");
    }

    // --- PENGATURAN FILTER DEWASA (Point 4) ---
    public void setSafeSearch(boolean isEnabled) {
        sharedPreferences.edit().putBoolean(KEY_SAFE_SEARCH, isEnabled).apply();
    }

    public boolean isSafeSearchEnabled() {
        // Default TRUE (Aktif) agar aman
        return sharedPreferences.getBoolean(KEY_SAFE_SEARCH, true);
    }
}
