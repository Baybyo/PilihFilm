package com.halil.ozel.moviedb.ui.home.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.SharedPreferences; // <-- IMPORT BARU
import android.content.Context; // <-- IMPORT BARU

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import com.google.android.material.appbar.MaterialToolbar;
import com.halil.ozel.moviedb.App;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.data.Api.TMDbAPI;
import com.halil.ozel.moviedb.data.models.TvResults;
import com.halil.ozel.moviedb.ui.home.adapters.TvSeriesAdapter;
import com.halil.ozel.moviedb.data.FavoritesManager; // 🆕 Import FavoritesManager

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;

public class AllTvActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_TITLE = "title";

    @Inject
    TMDbAPI tmDbAPI;

    private final List<TvResults> tvList = new ArrayList<>();
    private TvSeriesAdapter adapter;
    private FavoritesManager favoritesManager; // 🆕 Deklarasi manager
    private int currentPage = 1;
    private int totalPages = Integer.MAX_VALUE;
    private boolean isLoading = false;

    // --- KEY PREFERENCES BARU ---
    private String regionCode;
    private static final String PREFS = "settings";
    private static final String KEY_REGION = "content_region";
    // --- AKHIR KEY BARU ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.instance().appComponent().inject(this);
        setContentView(R.layout.activity_all_tv);

        // --- BARU: Baca SharedPreferences untuk Wilayah ---
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        // Ambil region, default ke "ID" (Indonesia) jika tidak ada
        regionCode = prefs.getString(KEY_REGION, "ID");
        // --- AKHIR BLOK BARU ---

        // 🆕 Inisialisasi FavoritesManager
        favoritesManager = new FavoritesManager();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);        toolbar.setTitle(getIntent().getStringExtra(EXTRA_TITLE));

        RecyclerView rv = findViewById(R.id.rvAllTv);
        // ✅ Perbaikan: Kirim 3 argumen ke TvSeriesAdapter
        adapter = new TvSeriesAdapter(tvList, this, favoritesManager);
        GridLayoutManager manager = new GridLayoutManager(this, 3);
        rv.setLayoutManager(manager);
        rv.setAdapter(adapter);

        rv.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int lastVisible = manager.findLastVisibleItemPosition();
                if (!isLoading && currentPage <= totalPages && lastVisible >= tvList.size() - 4) {
                    loadTv();
                }
            }
        });

        loadTv();
    }

    private void loadTv() {
        String category = getIntent().getStringExtra(EXTRA_CATEGORY);
        Observable<? extends com.halil.ozel.moviedb.data.models.ResponseTvSeries> call;
        if ("top_rated".equals(category)) {
            call = tmDbAPI.getTvTopRated(TMDb_API_KEY, currentPage, regionCode);
        } else if ("airing_today".equals(category)) {
            call = tmDbAPI.getTvAiringToday(TMDb_API_KEY, currentPage, regionCode);
        } else if ("on_the_air".equals(category)) {
            call = tmDbAPI.getTvOnTheAir(TMDb_API_KEY, currentPage, regionCode);
        } else { // "popular"
            call = tmDbAPI.getTvPopular(TMDb_API_KEY, currentPage, regionCode);
        }
        isLoading = true;
        call.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    totalPages = response.getTotal_pages();
                    tvList.addAll(response.getResults());
                    adapter.notifyDataSetChanged();
                    currentPage++;
                    isLoading = false;
                }, e -> {
                    Timber.e(e, "Error fetching tv: %s", e.getMessage());
                    isLoading = false;
                });
    }
}
//package com.halil.ozel.moviedb.ui.home.activity;
//
//import androidx.appcompat.app.AppCompatActivity;
//import android.os.Bundle;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
//
//import com.google.android.material.appbar.MaterialToolbar;
//import com.halil.ozel.moviedb.App;
//import com.halil.ozel.moviedb.R;
//import com.halil.ozel.moviedb.data.Api.TMDbAPI;
//import com.halil.ozel.moviedb.data.models.TvResults;
//import com.halil.ozel.moviedb.ui.home.adapters.TvSeriesAdapter;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.inject.Inject;
//
//import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
//import io.reactivex.rxjava3.core.Observable;
//import io.reactivex.rxjava3.schedulers.Schedulers;
//import timber.log.Timber;
//
//import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;
//
//public class AllTvActivity extends AppCompatActivity {
//
//    public static final String EXTRA_CATEGORY = "category";
//    public static final String EXTRA_TITLE = "title";
//
//    @Inject
//    TMDbAPI tmDbAPI;
//
//    private final List<TvResults> tvList = new ArrayList<>();
//    private TvSeriesAdapter adapter;
//    private int currentPage = 1;
//    private int totalPages = Integer.MAX_VALUE;
//    private boolean isLoading = false;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        App.instance().appComponent().inject(this);
//        setContentView(R.layout.activity_all_tv);
//
//        MaterialToolbar toolbar = findViewById(R.id.toolbar);
//        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
//        toolbar.setNavigationOnClickListener(v -> onBackPressed());
//        toolbar.setTitle(getIntent().getStringExtra(EXTRA_TITLE));
//
//        RecyclerView rv = findViewById(R.id.rvAllTv);
//        adapter = new TvSeriesAdapter(tvList, this);
//        GridLayoutManager manager = new GridLayoutManager(this, 3);
//        rv.setLayoutManager(manager);
//        rv.setAdapter(adapter);
//
//        rv.addOnScrollListener(new OnScrollListener() {
//            @Override
//            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//                int lastVisible = manager.findLastVisibleItemPosition();
//                if (!isLoading && currentPage <= totalPages && lastVisible >= tvList.size() - 4) {
//                    loadTv();
//                }
//            }
//        });
//
//        loadTv();
//    }
//
//    private void loadTv() {
//        String category = getIntent().getStringExtra(EXTRA_CATEGORY);
//        Observable<? extends com.halil.ozel.moviedb.data.models.ResponseTvSeries> call;
//        if ("top_rated".equals(category)) {
//            call = tmDbAPI.getTvTopRated(TMDb_API_KEY, currentPage);
//        } else if ("airing_today".equals(category)) {
//            call = tmDbAPI.getTvAiringToday(TMDb_API_KEY, currentPage);
//        } else if ("on_the_air".equals(category)) {
//            call = tmDbAPI.getTvOnTheAir(TMDb_API_KEY, currentPage);
//        } else {
//            call = tmDbAPI.getTvPopular(TMDb_API_KEY, currentPage);
//        }
//        isLoading = true;
//        call.subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(response -> {
//                    totalPages = response.getTotal_pages();
//                    tvList.addAll(response.getResults());
//                    adapter.notifyDataSetChanged();
//                    currentPage++;
//                    isLoading = false;
//                }, e -> {
//                    Timber.e(e, "Error fetching tv: %s", e.getMessage());
//                    isLoading = false;
//                });
//    }
//}
