package com.halil.ozel.moviedb.ui.home.fragments;

import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.halil.ozel.moviedb.App;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.data.Api.TMDbAPI;
import com.halil.ozel.moviedb.data.FavoritesManager;
import com.halil.ozel.moviedb.data.models.Cast;
import com.halil.ozel.moviedb.data.models.Results;
import com.halil.ozel.moviedb.data.models.TvResults;
import com.halil.ozel.moviedb.ui.detail.CastDetailActivity;
import com.halil.ozel.moviedb.ui.detail.MovieDetailActivity;
import com.halil.ozel.moviedb.ui.detail.TvSeriesDetailActivity;
import com.halil.ozel.moviedb.ui.detail.adapters.MovieCastAdapter;
import com.halil.ozel.moviedb.ui.home.adapters.MovieAdapter;
import com.halil.ozel.moviedb.ui.home.adapters.SearchHistoryAdapter;
import com.halil.ozel.moviedb.ui.home.adapters.TvSeriesAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class SearchFragment extends Fragment implements SearchHistoryAdapter.OnHistoryItemClickListener {

    @Inject
    TMDbAPI tmDbAPI;

    // --- Views UI ---
    private EditText etQuery;
    private ImageView ivClearSearch;
    private ProgressBar pbSearch;
    private LinearLayout layoutEmptyState;
    private NestedScrollView svResults;

    // Titles
    private TextView tvMovieResultTitle, tvTvResultTitle, tvPersonResultTitle;

    // RecyclerViews
    private RecyclerView rvMovieResults, rvTvResults, rvPersonResults;
    private RecyclerView rvSearchHistory;
    private LinearLayout llSearchHistory;

    // Adapters
    private MovieAdapter movieAdapter;
    private TvSeriesAdapter tvAdapter;
    private MovieCastAdapter personAdapter; // Kita gunakan MovieCastAdapter untuk orang
    private SearchHistoryAdapter historyAdapter;

    // Data Lists
    private final List<Results> movieList = new ArrayList<>();
    private final List<TvResults> tvList = new ArrayList<>();
    private final List<Cast> personList = new ArrayList<>();
    private final List<String> historyList = new ArrayList<>();

    // --- Preferences & Utils ---
    private static final String PREFS = "settings";
    private static final String KEY_ADULT = "include_adult";
    private static final String KEY_REGION = "content_region";
    private static final String SEARCH_HISTORY_PREFS = "search_history_prefs";
    private static final String KEY_HISTORY_SET = "search_history_set";

    private String languageCode;
    private String regionCode;
    private FavoritesManager favoritesManager;
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        App.instance().appComponent().inject(this);
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        favoritesManager = new FavoritesManager();

        // 1. Init Preferences
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        languageCode = prefs.getString("language", "en-US");
        regionCode = prefs.getString(KEY_REGION, "ID");

        // 2. Init Views
        etQuery = view.findViewById(R.id.etQuery);
        ivClearSearch = view.findViewById(R.id.ivClearSearch);
        pbSearch = view.findViewById(R.id.pbSearch);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        svResults = view.findViewById(R.id.svResults);

        llSearchHistory = view.findViewById(R.id.llSearchHistory);
        rvSearchHistory = view.findViewById(R.id.rvSearchHistory);

        rvMovieResults = view.findViewById(R.id.rvMovieResults);
        rvTvResults = view.findViewById(R.id.rvTvResults);
        rvPersonResults = view.findViewById(R.id.rvPersonResults);

        tvMovieResultTitle = view.findViewById(R.id.tvMovieResultTitle);
        tvTvResultTitle = view.findViewById(R.id.tvTvResultTitle);
        tvPersonResultTitle = view.findViewById(R.id.tvPersonResultTitle);

        // 3. Setup Adapters
        setupAdapters();

        // 4. Listeners
        setupSearchListeners();

        // Tampilkan history awal
        loadSearchHistory();
    }

    private void setupAdapters() {
        // Movie Adapter
        rvMovieResults.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        movieAdapter = new MovieAdapter(movieList, getContext(), favoritesManager);
        rvMovieResults.setAdapter(movieAdapter);

        // TV Adapter
        rvTvResults.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        tvAdapter = new TvSeriesAdapter(tvList, getContext(), favoritesManager);
        rvTvResults.setAdapter(tvAdapter);

        // Person Adapter (Reuse MovieCastAdapter for search results too)
        rvPersonResults.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        personAdapter = new MovieCastAdapter(personList, getContext());
        rvPersonResults.setAdapter(personAdapter);

        // History Adapter
        rvSearchHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new SearchHistoryAdapter(historyList, this);
        rvSearchHistory.setAdapter(historyAdapter);
    }

    private void setupSearchListeners() {
        etQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etQuery.getText().toString().trim();
                if (!query.isEmpty()) {
                    performMultiSearch(query);
                    saveSearchQuery(query);
                    hideKeyboard();
                    showHistoryView(false);
                }
                return true;
            }
            return false;
        });

        etQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (s.length() == 0) {
                    showHistoryView(true);
                    clearAllResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivClearSearch.setOnClickListener(v -> {
            etQuery.setText("");
            clearAllResults();
            showHistoryView(true);
        });
    }

    private void performMultiSearch(String query) {
        pbSearch.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        svResults.setVisibility(View.GONE);

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean includeAdult = prefs.getBoolean(KEY_ADULT, false);
        regionCode = prefs.getString(KEY_REGION, "ID");

        // Clear previous results
        movieList.clear();
        tvList.clear();
        personList.clear();

        // 1. Search Movies
        disposable.add(tmDbAPI.searchMovie(TMDb_API_KEY, query, 1, languageCode, includeAdult, regionCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.getResults() != null) {
                        movieList.addAll(response.getResults());
                        movieAdapter.notifyDataSetChanged();
                    }
                    checkAllLoaded(); // Cek apakah semua selesai
                }, e -> Timber.e(e)));

        // 2. Search TV
        disposable.add(tmDbAPI.searchTv(TMDb_API_KEY, query, 1, languageCode, includeAdult, regionCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.getResults() != null) {
                        tvList.addAll(response.getResults());
                        tvAdapter.notifyDataSetChanged();
                    }
                    checkAllLoaded();
                }, e -> Timber.e(e)));

        // 3. Search Person
        disposable.add(tmDbAPI.searchPerson(TMDb_API_KEY, query, 1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.getResults() != null) {
                        personList.addAll(response.getResults());
                        personAdapter.notifyDataSetChanged();
                    }
                    checkAllLoaded();
                }, e -> Timber.e(e)));
    }

    private void checkAllLoaded() {
        // Sederhana: Kita panggil ini setiap kali salah satu request selesai.
        // Di aplikasi nyata, bisa pakai zip/merge operator RxJava.
        // Tapi ini cukup untuk update UI secara reaktif.

        pbSearch.setVisibility(View.GONE);

        boolean hasMovies = !movieList.isEmpty();
        boolean hasTv = !tvList.isEmpty();
        boolean hasPerson = !personList.isEmpty();

        if (hasMovies || hasTv || hasPerson) {
            svResults.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);

            // Atur Visibilitas Section
            tvMovieResultTitle.setVisibility(hasMovies ? View.VISIBLE : View.GONE);
            rvMovieResults.setVisibility(hasMovies ? View.VISIBLE : View.GONE);

            tvTvResultTitle.setVisibility(hasTv ? View.VISIBLE : View.GONE);
            rvTvResults.setVisibility(hasTv ? View.VISIBLE : View.GONE);

            tvPersonResultTitle.setVisibility(hasPerson ? View.VISIBLE : View.GONE);
            rvPersonResults.setVisibility(hasPerson ? View.VISIBLE : View.GONE);
        } else {
            svResults.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private void clearAllResults() {
        movieList.clear();
        tvList.clear();
        personList.clear();
        movieAdapter.notifyDataSetChanged();
        tvAdapter.notifyDataSetChanged();
        personAdapter.notifyDataSetChanged();
        svResults.setVisibility(View.GONE);
    }

    // --- History Logic (Sama seperti sebelumnya) ---

    private void loadSearchHistory() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(SEARCH_HISTORY_PREFS, Context.MODE_PRIVATE);
        Set<String> historySet = prefs.getStringSet(KEY_HISTORY_SET, new HashSet<>());

        historyList.clear();
        historyList.addAll(historySet);
        Collections.sort(historyList); // Urutkan alfabetis (opsional)
        historyAdapter.notifyDataSetChanged();

        if (etQuery.getText().toString().isEmpty()) {
            showHistoryView(true);
        }
    }

    private void saveSearchQuery(String query) {
        if (getContext() == null || query.isEmpty()) return;
        SharedPreferences prefs = getContext().getSharedPreferences(SEARCH_HISTORY_PREFS, Context.MODE_PRIVATE);
        Set<String> historySet = new HashSet<>(prefs.getStringSet(KEY_HISTORY_SET, new HashSet<>()));
        historySet.add(query.trim().toLowerCase());
        prefs.edit().putStringSet(KEY_HISTORY_SET, historySet).apply();
        loadSearchHistory();
    }

    private void showHistoryView(boolean show) {
        if (llSearchHistory == null) return;
        if (show && !historyList.isEmpty()) {
            llSearchHistory.setVisibility(View.VISIBLE);
            svResults.setVisibility(View.GONE);
        } else {
            llSearchHistory.setVisibility(View.GONE);
        }
    }

    private void hideKeyboard() {
        if (getActivity() != null && getActivity().getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onHistoryItemClick(String query) {
        etQuery.setText(query);
        etQuery.setSelection(query.length());
        performMultiSearch(query);
        hideKeyboard();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposable.clear();
    }
}