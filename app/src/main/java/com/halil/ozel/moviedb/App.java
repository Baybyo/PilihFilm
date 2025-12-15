package com.halil.ozel.moviedb;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build; // Tambahan import untuk Build
import java.util.Locale;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.halil.ozel.moviedb.dagger.components.ApplicationComponent;
import com.halil.ozel.moviedb.dagger.components.DaggerApplicationComponent;
import com.halil.ozel.moviedb.dagger.modules.ApplicationModule;
import com.halil.ozel.moviedb.dagger.modules.HttpClientModule;

import timber.log.Timber;

public class App extends Application {

    private static App instance;
    private ApplicationComponent mApplicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean dark = prefs.getBoolean("dark", false);
        AppCompatDelegate.setDefaultNightMode(dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        String lang = prefs.getString("language", "en");
        applyLocale(lang);

        // Setup Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        // Creates Dagger Graph
        mApplicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .httpClientModule(new HttpClientModule())
                .build();

        mApplicationComponent.inject(this);
    }

    public static App instance() {
        return instance;
    }

    public ApplicationComponent appComponent() {
        return mApplicationComponent;
    }

    // [PERBAIKAN DIMULAI]
    @SuppressLint("NewApi") // Suppress warning karena kita sudah handle manual dengan if/else
    public void applyLocale(String lang) {
        Locale locale;

        // Cek apakah versi Android >= Lollipop (API 21)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Cara baru yang direkomendasikan
            locale = Locale.forLanguageTag(lang);
        } else {
            // Cara lama untuk kompatibilitas API < 21
            locale = new Locale(lang);
        }

        // Tetapkan Locale ke sistem aplikasi
        Locale.setDefault(locale);
        LocaleListCompat locales = LocaleListCompat.create(locale);
        AppCompatDelegate.setApplicationLocales(locales);
    }
    // [PERBAIKAN BERAKHIR]
}