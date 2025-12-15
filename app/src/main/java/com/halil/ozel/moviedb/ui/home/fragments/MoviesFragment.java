package com.halil.ozel.moviedb.ui.home.fragments;

import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;
import static com.halil.ozel.moviedb.data.Api.TMDbAPI.IMAGE_BASE_URL_1280; // Import untuk backdrop

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Import untuk ImageView (Hero Banner)

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.halil.ozel.moviedb.App;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.data.Api.TMDbAPI;
import com.halil.ozel.moviedb.data.FavoritesManager;
import com.halil.ozel.moviedb.data.models.Results;
import com.halil.ozel.moviedb.ui.home.activity.AllMoviesActivity;
import com.halil.ozel.moviedb.ui.home.adapters.MovieAdapter;
import com.bumptech.glide.Glide; // Import Glide
import com.squareup.picasso.Picasso; // Import Picasso

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MoviesFragment extends Fragment {

    @Inject
    TMDbAPI tmDbAPI;

    // --- RecyclerViews & Adapters ---
    private RecyclerView rvPopularMovie, rvNowPlaying, rvTopRated, rvUpcoming;
    private MovieAdapter popularMovieAdapter, nowPlayingMovieAdapter, topRatedMovieAdapter, upcomingMovieAdapter;

    // 🆕 VARIABEL BARU UNTUK HERO BANNER
    private ImageView imgHeroBanner;

    // --- Data Lists ---
    private final List<Results> popularMovieDataList = new ArrayList<>();
    private final List<Results> nowPlayingDataList = new ArrayList<>();
    private final List<Results> topRatedDataList = new ArrayList<>();
    private final List<Results> upcomingDataList = new ArrayList<>();

    // --- Utils ---
    private FavoritesManager favoritesManager;
    private final CompositeDisposable disposable = new CompositeDisposable();

    // --- Preferences Keys & Vars ---
    private static final String PREFS = "settings";
    private static final String KEY_REGION = "content_region";
    private static final String KEY_ADULT = "include_adult";
    private String regionCode;
    private boolean includeAdult;

    public MoviesFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movies, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inject Dagger
        App.instance().appComponent().inject(this);

        // 2. Init Preferences
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        regionCode = prefs.getString(KEY_REGION, "ID");
        includeAdult = prefs.getBoolean(KEY_ADULT, false);

        // 3. Init Favorites Manager
        favoritesManager = new FavoritesManager();

        // 4. 🆕 INISIALISASI HERO BANNER
        // Anda harus memastikan ID 'img_hero_banner' ditambahkan ke fragment_movies.xml
        // Saya akan menggunakan R.id.imgHeroBanner
        // Jika Hero Banner tidak ada, ImageView akan bernilai null
        imgHeroBanner = view.findViewById(R.id.img_hero_banner);

        // 5. Setup RecyclerViews
        rvPopularMovie = setupRecyclerView(view.findViewById(R.id.rvPopularMovie));
        popularMovieAdapter = new MovieAdapter(popularMovieDataList, getContext(), favoritesManager);
        rvPopularMovie.setAdapter(popularMovieAdapter);

        rvNowPlaying = setupRecyclerView(view.findViewById(R.id.rvNowPlaying));
        nowPlayingMovieAdapter = new MovieAdapter(nowPlayingDataList, getContext(), favoritesManager);
        rvNowPlaying.setAdapter(nowPlayingMovieAdapter);

        rvTopRated = setupRecyclerView(view.findViewById(R.id.rvTopRated));
        topRatedMovieAdapter = new MovieAdapter(topRatedDataList, getContext(), favoritesManager);
        rvTopRated.setAdapter(topRatedMovieAdapter);

        rvUpcoming = setupRecyclerView(view.findViewById(R.id.rvUpcoming));
        upcomingMovieAdapter = new MovieAdapter(upcomingDataList, getContext(), favoritesManager);
        rvUpcoming.setAdapter(upcomingMovieAdapter);

        // 6. Setup Listeners "See All"
        view.findViewById(R.id.tvNowPlayingSeeAll).setOnClickListener(v -> startAll("now_playing", "Now Playing"));
        view.findViewById(R.id.tvPopularSeeAll).setOnClickListener(v -> startAll("popular", "Popular"));
        view.findViewById(R.id.tvTopRatedSeeAll).setOnClickListener(v -> startAll("top_rated", "Top Rated"));
        view.findViewById(R.id.tvUpcomingSeeAll).setOnClickListener(v -> startAll("upcoming", "Upcoming"));

        // 7. Load Data Awal
        loadAllMovies();
    }

    private RecyclerView setupRecyclerView(RecyclerView rv) {
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rv.setNestedScrollingEnabled(false);
        return rv;
    }

    private void loadAllMovies() {
        getNowPlaying();
        getPopularMovies();
        getTopRatedMovies();
        getUpcomingMovies();
    }

    // --- API Calls ---

    public void getNowPlaying() {
        disposable.add(tmDbAPI.getNowPlaying(TMDb_API_KEY, 1, regionCode, includeAdult)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    nowPlayingDataList.clear();
                    nowPlayingDataList.addAll(response.getResults());
                    if (nowPlayingMovieAdapter != null) {
                        nowPlayingMovieAdapter.notifyDataSetChanged();
                    }
                }, e -> Timber.e(e, "Error fetching now playing movies: %s", e.getMessage())));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void getPopularMovies() {
        disposable.add(tmDbAPI.getPopularMovie(TMDb_API_KEY, 1, regionCode, includeAdult)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    popularMovieDataList.clear();
                    popularMovieDataList.addAll(response.getResults());

                    // 🆕 LOGIC HERO BANNER BARU (Dari kode yang diminta digabungkan)
                    if (!popularMovieDataList.isEmpty() && imgHeroBanner != null) {
                        Results heroMovie = popularMovieDataList.get(0);
                        String backdropPath = heroMovie.getBackdrop_path();

                        if (backdropPath != null && getContext() != null) {
                            // Menggunakan Glide/Picasso untuk loading Hero Banner
                            Picasso.get()
                                    .load(IMAGE_BASE_URL_1280 + backdropPath)
                                    .into(imgHeroBanner);
                        }
                    }
                    // ----------------------------------------------------

                    if (popularMovieAdapter != null) {
                        popularMovieAdapter.notifyDataSetChanged();
                    }
                }, e -> Timber.e(e, "Error fetching popular movies: %s", e.getMessage())));
    }

    public void getTopRatedMovies() {
        disposable.add(tmDbAPI.getTopRatedMovie(TMDb_API_KEY, 1, regionCode, includeAdult)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    topRatedDataList.clear();
                    topRatedDataList.addAll(response.getResults());
                    if (topRatedMovieAdapter != null) {
                        topRatedMovieAdapter.notifyDataSetChanged();
                    }
                }, e -> Timber.e(e, "Error fetching top rated movies: %s", e.getMessage())));
    }

    public void getUpcomingMovies() {
        disposable.add(tmDbAPI.getUpcomingMovie(TMDb_API_KEY, 1, regionCode, includeAdult)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    upcomingDataList.clear();
                    upcomingDataList.addAll(response.getResults());
                    if (upcomingMovieAdapter != null) {
                        upcomingMovieAdapter.notifyDataSetChanged();
                    }
                }, e -> Timber.e(e, "Error fetching upcoming movies: %s", e.getMessage())));
    }

    // --- Lifecycle & Helpers ---

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() == null) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String newRegionCode = prefs.getString(KEY_REGION, "ID");
        boolean newIncludeAdult = prefs.getBoolean(KEY_ADULT, false);

        if (!newRegionCode.equals(regionCode) || newIncludeAdult != includeAdult) {
            regionCode = newRegionCode;
            includeAdult = newIncludeAdult;
            loadAllMovies();
        } else {
            if(popularMovieAdapter != null) popularMovieAdapter.notifyDataSetChanged();
            if(nowPlayingMovieAdapter != null) nowPlayingMovieAdapter.notifyDataSetChanged();
            if(topRatedMovieAdapter != null) topRatedMovieAdapter.notifyDataSetChanged();
            if(upcomingMovieAdapter != null) upcomingMovieAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposable.clear();
        popularMovieAdapter = null;
        nowPlayingMovieAdapter = null;
        topRatedMovieAdapter = null;
        upcomingMovieAdapter = null;
    }

    private void startAll(String category, String title) {
        Intent intent = new Intent(getContext(), AllMoviesActivity.class);
        intent.putExtra(AllMoviesActivity.EXTRA_CATEGORY, category);
        intent.putExtra(AllMoviesActivity.EXTRA_TITLE, title);
        startActivity(intent);
    }
}