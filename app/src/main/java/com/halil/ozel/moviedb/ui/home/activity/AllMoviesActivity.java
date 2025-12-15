package com.halil.ozel.moviedb.ui.home.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import com.google.android.material.appbar.MaterialToolbar;
import com.halil.ozel.moviedb.App;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.data.Api.TMDbAPI;
import com.halil.ozel.moviedb.data.models.Results;
import com.halil.ozel.moviedb.ui.home.adapters.MovieAdapter;
import com.halil.ozel.moviedb.data.FavoritesManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;

public class AllMoviesActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_TITLE = "title";

    @Inject
    TMDbAPI tmDbAPI;

    private final List<Results> movieList = new ArrayList<>();
    private MovieAdapter adapter;
    private FavoritesManager favoritesManager;
    private int currentPage = 1;
    private int totalPages = Integer.MAX_VALUE;
    private boolean isLoading = false;

    // --- KEY PREFERENCES BARU ---
    private String regionCode;
    private boolean includeAdult; // 🆕 Deklarasi includeAdult
    private static final String PREFS = "settings";
    private static final String KEY_REGION = "content_region";
    private static final String KEY_ADULT = "include_adult"; // 🆕 Key untuk Konten Dewasa
    // --- AKHIR KEY BARU ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.instance().appComponent().inject(this);
        setContentView(R.layout.activity_all_movies);

        // --- BACA SharedPreferences (Wilayah & Konten Dewasa) ---
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        regionCode = prefs.getString(KEY_REGION, "ID");
        includeAdult = prefs.getBoolean(KEY_ADULT, false); // 🆕 Baca status filter dewasa
        // --- AKHIR BLOK BACA PREFS ---

        favoritesManager = new FavoritesManager();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setTitle(getIntent().getStringExtra(EXTRA_TITLE));

        RecyclerView rv = findViewById(R.id.rvAllMovies);
        adapter = new MovieAdapter(movieList, this, favoritesManager);
        GridLayoutManager manager = new GridLayoutManager(this, 3);
        rv.setLayoutManager(manager);
        rv.setAdapter(adapter);

        rv.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int lastVisible = manager.findLastVisibleItemPosition();
                // Menggunakan >= movieList.size() - 6 (lebih aman)
                if (!isLoading && currentPage <= totalPages && lastVisible >= movieList.size() - 6) {
                    loadMovies();
                }
            }
        });

        loadMovies();
    }

    // Nama metode yang digunakan di OnScrollListener
    private void loadMovies() {
        String category = getIntent().getStringExtra(EXTRA_CATEGORY);
        Observable<com.halil.ozel.moviedb.data.models.ResponseNowPlaying> call = getMovieCall(category, currentPage);

        if (call == null) return; // Safegard jika kategori tidak valid

        isLoading = true;
        call.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    totalPages = response.getTotal_pages();
                    movieList.addAll(response.getResults());
                    adapter.notifyDataSetChanged();
                    currentPage++;
                    isLoading = false;
                }, e -> {
                    Timber.e(e, "Error fetching movies: %s", e.getMessage());
                    isLoading = false;
                });
    }

    // 🆕 METODE BARU: Menggantikan logika switch di loadMovies
    private Observable<com.halil.ozel.moviedb.data.models.ResponseNowPlaying> getMovieCall(String category, int page) {

        // Asumsi category sudah diambil dari Intent (EXTRA_CATEGORY)
        switch (category) {
            case "now_playing":
                // Panggil dengan 4 argumen (KEY, page, regionCode, includeAdult)
                return tmDbAPI.getNowPlaying(TMDb_API_KEY, page, regionCode, includeAdult);
            case "top_rated":
                // Panggil dengan 4 argumen (KEY, page, regionCode, includeAdult)
                return tmDbAPI.getTopRatedMovie(TMDb_API_KEY, page, regionCode, includeAdult);
            case "upcoming":
                // Panggil dengan 4 argumen (KEY, page, regionCode, includeAdult)
                return tmDbAPI.getUpcomingMovie(TMDb_API_KEY, page, regionCode, includeAdult);
            case "popular":
            default:
                // Panggil dengan 4 argumen (KEY, page, regionCode, includeAdult)
                return tmDbAPI.getPopularMovie(TMDb_API_KEY, page, regionCode, includeAdult);
        }
    }
}