package com.halil.ozel.moviedb.ui.detail;

import static com.halil.ozel.moviedb.data.Api.TMDbAPI.IMAGE_BASE_URL_1280;
import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build; // [TAMBAHAN IMPORT]
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.halil.ozel.moviedb.App;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.data.Api.TMDbAPI;
import com.halil.ozel.moviedb.data.FavoritesManager;
import com.halil.ozel.moviedb.data.models.Cast;
import com.halil.ozel.moviedb.data.models.Genres;
import com.halil.ozel.moviedb.data.models.Results;
import com.halil.ozel.moviedb.data.models.VideoResult;
import com.halil.ozel.moviedb.ui.detail.adapters.MovieCastAdapter;
import com.halil.ozel.moviedb.ui.home.adapters.MovieAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import at.blogc.android.views.ExpandableTextView;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MovieDetailActivity extends AppCompatActivity {

    // Variabel UI
    String title;
    int id;

    // Disesuaikan dengan XML baru
    ImageView ivBackdrop;
    TextView tvTitle, tvRating, tvReleaseDate, tvRelatedLabel;
    ExpandableTextView tvOverview;
    ChipGroup chipGroupGenres;

    Button btnWatchTrailer;
    FloatingActionButton fabFavorite;
    Toolbar toolbar;

    Button btnPlay;

    // Logic Variables
    private String languageCode;
    private String trailerKey = null;
    private FavoritesManager favoritesManager;

    @Inject
    TMDbAPI tmDbAPI;

    // RecyclerViews
    public RecyclerView rvCast, rvRecommendations;
    public RecyclerView.Adapter castAdapter, recommendAdapter;
    public List<Cast> castDataList;
    public List<Results> recommendDataList;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.instance().appComponent().inject(this);
        setContentView(R.layout.activity_movie_detail);

        // 1. Inisialisasi FavoritesManager
        favoritesManager = new FavoritesManager();

        // 2. Setup Settings & Language
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        languageCode = prefs.getString("language", "en-US");

        // 3. Binding Views
        ivBackdrop = findViewById(R.id.ivMovieBackdrop);
        tvTitle = findViewById(R.id.tvMovieTitle);
        tvRating = findViewById(R.id.tvMovieRating);
        tvReleaseDate = findViewById(R.id.tvMovieReleaseDate);
        tvRelatedLabel = findViewById(R.id.tvRelatedLabel);
        tvOverview = findViewById(R.id.tvOverview);
        chipGroupGenres = findViewById(R.id.chipGroupGenres);
        fabFavorite = findViewById(R.id.fabFavorite);
        btnWatchTrailer = findViewById(R.id.btnWatchTrailer);
        toolbar = findViewById(R.id.toolbar);
//        btnPlay = findViewById(R.id.btnPlay);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // 4. Setup RecyclerView Cast
        castDataList = new ArrayList<>();
        castAdapter = new MovieCastAdapter(castDataList, this);
        rvCast = findViewById(R.id.rvCast);
        rvCast.setHasFixedSize(true);
        rvCast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCast.setAdapter(castAdapter);

        // 5. Setup RecyclerView Recommendations
        recommendDataList = new ArrayList<>();
        recommendAdapter = new MovieAdapter(recommendDataList, this, favoritesManager);
        rvRecommendations = findViewById(R.id.rvRecommendations);
        rvRecommendations.setHasFixedSize(true);
        rvRecommendations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRecommendations.setAdapter(recommendAdapter);

        // 6. Setup Expandable TextView
        tvOverview.setAnimationDuration(750L);
        tvOverview.setInterpolator(new OvershootInterpolator());
        tvOverview.setOnClickListener(v -> tvOverview.toggle());

        // 7. Get Intent Data & Set UI
        title = getIntent().getStringExtra("title");
        id = getIntent().getIntExtra("id", 0);

        // --- Logic Sinopsis ---
        String overview = getIntent().getStringExtra("overview");
        if (overview == null || overview.isEmpty() || overview.equals("null")) {
            overview = "Sinopsis tidak tersedia dalam bahasa ini.";
        }
        tvOverview.setText(overview);

        tvTitle.setText(title);

        // Menampilkan Rating/Popularity
        double popularity = getIntent().getDoubleExtra("popularity", 0);
        tvRating.setText(String.valueOf(popularity));

        tvReleaseDate.setText(getIntent().getStringExtra("release_date"));

        // Load Image
        Picasso.get().load(IMAGE_BASE_URL_1280 + getIntent().getStringExtra("backdrop")).into(ivBackdrop);

        // 8. Handle Genres (Menggunakan CHIP)
        // [PERBAIKAN START: Handling getSerializableExtra deprecated]
        List<Genres> labelPS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            labelPS = getIntent().getSerializableExtra("genres", ArrayList.class);
        } else {
            labelPS = (List<Genres>) getIntent().getSerializableExtra("genres");
        }
        // [PERBAIKAN END]

        chipGroupGenres.removeAllViews();
        if (labelPS != null && !labelPS.isEmpty()) {
            for (Genres genre : labelPS) {
                if (genre == null) continue;
                Chip chip = new Chip(this);
                chip.setText(genre.getName());
                chip.setChipBackgroundColorResource(R.color.colorAccent);

                // Menggunakan ContextCompat (Dari perbaikan sebelumnya)
                chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));

                chipGroupGenres.addView(chip);
            }
        }

        // 9. Setup Trailer Button
        btnWatchTrailer.setVisibility(View.GONE);
        btnWatchTrailer.setOnClickListener(v -> {
            if (trailerKey != null) {
                Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + trailerKey));
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + trailerKey));
                try {
                    startActivity(appIntent);
                } catch (Exception ex) {
                    startActivity(webIntent);
                }
            } else {
                Toast.makeText(this, "Trailer not available", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> {
                Toast.makeText(this, "Memutar Film...", Toast.LENGTH_SHORT).show();
            });
        }

        // 10. Load Network Data
        getCastInfo();
        getRecommendMovie();
        getMovieVideos();

        // 11. Update Favorite Status
        updateFabStatus();
        fabFavorite.setOnClickListener(v -> handleFavoriteClick());
    }

    // --- API CALLS ---

    @SuppressLint("NotifyDataSetChanged")
    public void getCastInfo() {
        tmDbAPI.getCreditDetail(id, TMDb_API_KEY, languageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    castDataList.addAll(response.getCast());
                    castAdapter.notifyDataSetChanged();
                }, e -> Timber.e(e, "Error fetching cast: %s", e.getMessage()));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void getRecommendMovie() {
        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        String regionCode = prefs.getString("content_region", "ID");
        tmDbAPI.getRecommendDetail(id, TMDb_API_KEY, languageCode, regionCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    recommendDataList.clear();
                    recommendDataList.addAll(response.getResults());
                    recommendAdapter.notifyDataSetChanged();

                    if (recommendDataList.isEmpty()) {
                        tvRelatedLabel.setVisibility(View.GONE);
                        rvRecommendations.setVisibility(View.GONE);
                    } else {
                        tvRelatedLabel.setVisibility(View.VISIBLE);
                        rvRecommendations.setVisibility(View.VISIBLE);
                    }
                }, e -> Timber.e(e, "Error fetching recommendations: %s", e.getMessage()));
    }

    private void getMovieVideos() {
        tmDbAPI.getMovieVideos(id, TMDb_API_KEY)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.getResults() != null && !response.getResults().isEmpty()) {
                        for (VideoResult video : response.getResults()) {
                            if ("Trailer".equals(video.getType()) && "YouTube".equals(video.getSite())) {
                                trailerKey = video.getKey();
                                btnWatchTrailer.setVisibility(View.VISIBLE);
                                break;
                            }
                        }
                    }
                }, e -> {
                    Timber.e(e, "Error fetching videos: %s", e.getMessage());
                    btnWatchTrailer.setVisibility(View.GONE);
                });
    }

    // --- FAVORITES LOGIC ---

    private void updateFabStatus() {
        favoritesManager.isFavorite(id)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean isFavorite = task.getResult();
                        fabFavorite.setImageResource(isFavorite ? R.drawable.ic_star_filled_24 : R.drawable.ic_star_border_24);
                    } else {
                        fabFavorite.setImageResource(R.drawable.ic_star_border_24);
                    }
                });
    }

    private void handleFavoriteClick() {
        favoritesManager.isFavorite(id)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean currentlyFavorite = task.getResult();
                        if (currentlyFavorite) {
                            favoritesManager.remove(id)
                                    .addOnSuccessListener(aVoid -> {
                                        fabFavorite.setImageResource(R.drawable.ic_star_border_24);
                                        Toast.makeText(MovieDetailActivity.this, R.string.favorite_removed, Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Results r = new Results();
                            r.setId(id);
                            r.setTitle(title);
                            r.setPoster_path(getIntent().getStringExtra("poster"));
                            r.setBackdrop_path(getIntent().getStringExtra("backdrop"));
                            r.setOverview(getIntent().getStringExtra("overview"));
                            r.setPopularity(getIntent().getDoubleExtra("popularity", 0));
                            r.setRelease_date(getIntent().getStringExtra("release_date"));

                            favoritesManager.add(r)
                                    .addOnSuccessListener(aVoid -> {
                                        fabFavorite.setImageResource(R.drawable.ic_star_filled_24);
                                        Toast.makeText(MovieDetailActivity.this, R.string.favorite_added, Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                });
    }
}