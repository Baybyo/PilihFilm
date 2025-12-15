package com.halil.ozel.moviedb.ui.detail;

import static com.halil.ozel.moviedb.data.Api.TMDbAPI.IMAGE_BASE_URL_500;
import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.halil.ozel.moviedb.App;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.data.Api.TMDbAPI;
import com.halil.ozel.moviedb.data.FavoritesManager;
import com.halil.ozel.moviedb.data.models.PersonDetail;
import com.halil.ozel.moviedb.data.models.Results;
import com.halil.ozel.moviedb.data.models.TvResults;
import com.halil.ozel.moviedb.ui.home.adapters.MovieAdapter;
import com.halil.ozel.moviedb.ui.home.adapters.TvSeriesAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import at.blogc.android.views.ExpandableTextView;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class CastDetailActivity extends AppCompatActivity {
    private String languageCode;
    @Inject
    TMDbAPI tmDbAPI;

    private ImageView ivProfile;
    private TextView tvName;
    private TextView tvBirthday;
    private ExpandableTextView tvBiography;
    private Button btnBioToggle;
    private TextView tvMoviesTitle;
    private TextView tvTvTitle;
    private RecyclerView rvMovieCredits;
    private RecyclerView rvTvCredits;
    private MovieAdapter movieAdapter;
    private TvSeriesAdapter tvAdapter;
    private final List<Results> movieList = new ArrayList<>();
    private final List<TvResults> tvList = new ArrayList<>();

    private MaterialToolbar detailToolbar;
    private FavoritesManager favoritesManager;

    private int personId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.instance().appComponent().inject(this);
        setContentView(R.layout.activity_cast_detail);

        // Init Favorites Manager
        favoritesManager = new FavoritesManager();

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        languageCode = prefs.getString("language", "en-US");

        setupToolbar();
        initViews();
        setupRecyclerViews();
        setupExpandableText();

        personId = getIntent().getIntExtra("person_id", 0);
        loadPersonDetail();
        loadMovieCredits();
        loadTvCredits();
    }

    private void setupToolbar() {
        detailToolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(detailToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        detailToolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        ivProfile = findViewById(R.id.ivProfile);
        tvName = findViewById(R.id.tvName);
        tvBirthday = findViewById(R.id.tvBirthday);
        tvBiography = findViewById(R.id.tvBiography);
        btnBioToggle = findViewById(R.id.btnBioToggle);
        tvMoviesTitle = findViewById(R.id.tvMoviesTitle);
        tvTvTitle = findViewById(R.id.tvTvTitle);
        rvMovieCredits = findViewById(R.id.rvMovieCredits);
        rvTvCredits = findViewById(R.id.rvTvCredits);

        tvMoviesTitle.setVisibility(View.GONE);
        tvTvTitle.setVisibility(View.GONE);
    }

    private void setupRecyclerViews() {
        rvMovieCredits.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        movieAdapter = new MovieAdapter(movieList, this, favoritesManager);
        rvMovieCredits.setAdapter(movieAdapter);

        rvTvCredits.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tvAdapter = new TvSeriesAdapter(tvList, this, favoritesManager);
        rvTvCredits.setAdapter(tvAdapter);
    }

    private void setupExpandableText() {
        tvBiography.setAnimationDuration(500L);
        tvBiography.setInterpolator(new OvershootInterpolator());
        tvBiography.setExpandInterpolator(new OvershootInterpolator());
        tvBiography.setCollapseInterpolator(new OvershootInterpolator());

        btnBioToggle.setOnClickListener(v -> {
            if (tvBiography.isExpanded()) {
                tvBiography.collapse();
                btnBioToggle.setBackgroundResource(R.drawable.ic_expand_more);
            } else {
                tvBiography.expand();
                btnBioToggle.setBackgroundResource(R.drawable.ic_expand_less);
            }
        });
    }

    private void loadPersonDetail() {
        tmDbAPI.getPersonDetail(personId, TMDb_API_KEY, languageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::bindPerson,
                        e -> {
                            Timber.e(e, "Error fetching person detail: %s", e.getMessage());
                            Toast.makeText(this, R.string.error_loading_data, Toast.LENGTH_SHORT).show();
                        });
    }

    private void loadMovieCredits() {
        tmDbAPI.getPersonMovieCredits(personId, TMDb_API_KEY, languageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    movieList.addAll(response.getCast());
                    movieAdapter.notifyDataSetChanged();
                    tvMoviesTitle.setVisibility(movieList.isEmpty() ? View.GONE : View.VISIBLE);
                }, e -> Timber.e(e, "Error fetching movie credits: %s", e.getMessage()));
    }

    private void loadTvCredits() {
        tmDbAPI.getPersonTvCredits(personId, TMDb_API_KEY, languageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    tvList.addAll(response.getCast());
                    tvAdapter.notifyDataSetChanged();
                    tvTvTitle.setVisibility(tvList.isEmpty() ? View.GONE : View.VISIBLE);
                }, e -> Timber.e(e, "Error fetching tv credits: %s", e.getMessage()));
    }

    private void bindPerson(PersonDetail detail) {
        tvName.setText(detail.getName());
        if (detail.getBirthday() != null) {
            tvBirthday.setText("Born: " + detail.getBirthday());
        } else {
            tvBirthday.setVisibility(View.GONE);
        }

        tvBiography.setText(detail.getBiography());

        if (detail.getProfile_path() != null) {
            // Menggunakan gambar resolusi tinggi untuk header besar
            Picasso.get().load(IMAGE_BASE_URL_500 + detail.getProfile_path())
                    .placeholder(R.drawable.ic_person)
                    .into(ivProfile);
        }
    }
}