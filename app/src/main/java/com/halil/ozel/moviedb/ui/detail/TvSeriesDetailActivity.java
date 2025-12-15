package com.halil.ozel.moviedb.ui.detail;

import static com.halil.ozel.moviedb.data.Api.TMDbAPI.IMAGE_BASE_URL_1280;
import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.halil.ozel.moviedb.App;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.data.Api.TMDbAPI;
import com.halil.ozel.moviedb.data.FavoritesManager;
import com.halil.ozel.moviedb.data.models.Cast;
import com.halil.ozel.moviedb.data.models.Episode;
import com.halil.ozel.moviedb.data.models.Genres;
import com.halil.ozel.moviedb.data.models.ResponseSeasonDetail;
import com.halil.ozel.moviedb.data.models.Results;
import com.halil.ozel.moviedb.data.models.Season;
import com.halil.ozel.moviedb.data.models.TvResults;
import com.halil.ozel.moviedb.ui.detail.adapters.MovieCastAdapter;
import com.halil.ozel.moviedb.ui.home.adapters.TvSeriesAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import at.blogc.android.views.ExpandableTextView;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class TvSeriesDetailActivity extends AppCompatActivity {

    // Data Variables
    String title;
    int id;
    private String languageCode;

    // Views
    ImageView ivHorizontalPoster;
    // [DIHAPUS] ivVerticalPoster sudah tidak ada di layout baru
    TextView tvTitle, tvGenres, tvPopularity, tvReleaseDate, tvSeasons, tvEpisodes, tvRelated, tvCastTitle;
    AutoCompleteTextView spSeason, spEpisode;
    ExpandableTextView etvOverview;
    Button btnToggle;
    FloatingActionButton fabFavorite;
    MaterialToolbar detailToolbar;

    // RecyclerViews
    public RecyclerView rvCast, rvRecommendContents;
    public RecyclerView.Adapter castAdapter, recommendAdapter;
    public List<Cast> castDataList;
    public List<TvResults> recommendDataList;

    // Season & Episode Data
    public List<Season> seasonList;
    public List<Episode> episodeList;
    private int currentSeasonNumber;

    // Dependencies
    @Inject
    TMDbAPI tmDbAPI;

    private FavoritesManager favoritesManager;
    private final CompositeDisposable disposable = new CompositeDisposable();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.instance().appComponent().inject(this);
        setContentView(R.layout.activity_tv_series_detail);

        // 1. Inisialisasi Favorites & Settings
        favoritesManager = new FavoritesManager();
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        languageCode = prefs.getString("language", "en-US");

        // 2. Binding Views
        ivHorizontalPoster = findViewById(R.id.ivHorizontalPoster);
        // [DIHAPUS] ivVerticalPoster = findViewById(R.id.ivVerticalPoster);

        tvTitle = findViewById(R.id.tvTitle);
        tvGenres = findViewById(R.id.tvGenres);
        tvPopularity = findViewById(R.id.tvPopularity);
        tvReleaseDate = findViewById(R.id.tvReleaseDate);
        tvSeasons = findViewById(R.id.tvSeasons);
        tvEpisodes = findViewById(R.id.tvEpisodes);
        spSeason = findViewById(R.id.spSeason);
        spEpisode = findViewById(R.id.spEpisode);
        tvRelated = findViewById(R.id.tvRelated);
        tvCastTitle = findViewById(R.id.tvCast);
        etvOverview = findViewById(R.id.etvOverview);
        btnToggle = findViewById(R.id.btnToggle);
        fabFavorite = findViewById(R.id.fabFavorite);
        detailToolbar = findViewById(R.id.detailToolbar);
        rvCast = findViewById(R.id.rvCast);
        rvRecommendContents = findViewById(R.id.rvRecommendContents);

        // 3. Setup Toolbar
        setSupportActionBar(detailToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        detailToolbar.setNavigationOnClickListener(v -> onBackPressed());

        // 4. Setup RecyclerViews
        setupRecyclerViews();

        // 5. Setup ExpandableTextView
        setupOverviewAnimation();

        // 6. Ambil Data dari Intent
        getIntentData();

        // 7. Load Data Tambahan
        getCastInfo();
        getRecommendTv();
        setupSeasonSpinner();

        // 8. Setup Favorit
        updateFabStatus();
        fabFavorite.setOnClickListener(v -> handleFavoriteClick());
    }

    private void setupRecyclerViews() {
        // Cast
        castDataList = new ArrayList<>();
        castAdapter = new MovieCastAdapter(castDataList, this);
        rvCast.setHasFixedSize(true);
        rvCast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCast.setAdapter(castAdapter);

        // Recommendations
        recommendDataList = new ArrayList<>();
        recommendAdapter = new TvSeriesAdapter(recommendDataList, this, favoritesManager);
        rvRecommendContents.setHasFixedSize(true);
        rvRecommendContents.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRecommendContents.setAdapter(recommendAdapter);
    }

    private void setupOverviewAnimation() {
        etvOverview.setAnimationDuration(750L);
        etvOverview.setInterpolator(new OvershootInterpolator());
        etvOverview.setExpandInterpolator(new OvershootInterpolator());
        etvOverview.setCollapseInterpolator(new OvershootInterpolator());

        btnToggle.setOnClickListener(v -> {
            if (etvOverview.isExpanded()) {
                etvOverview.collapse();
                btnToggle.setBackgroundResource(R.drawable.ic_expand_more);
            } else {
                etvOverview.expand();
                btnToggle.setBackgroundResource(R.drawable.ic_expand_less);
            }
        });
    }

    private void getIntentData() {
        title = getIntent().getStringExtra("title");
        id = getIntent().getIntExtra("id", 0);

        tvTitle.setText(title);
        etvOverview.setText(getIntent().getStringExtra("overview"));

        double pop = getIntent().getDoubleExtra("popularity", 0);
        tvPopularity.setText(String.format("%.1f", pop));

        tvReleaseDate.setText(getIntent().getStringExtra("release_date"));

        int seasons = getIntent().getIntExtra("seasons", 0);
        int episodes = getIntent().getIntExtra("episodes", 0);
        tvSeasons.setText(getString(R.string.seasons_format, seasons));
        tvEpisodes.setText(getString(R.string.episodes_format, episodes));

        // Load Images using Picasso
        String backdropPath = getIntent().getStringExtra("backdrop");

        // Logic Backdrop: Jika backdrop ada, pakai. Jika tidak, coba pakai poster path
        if (backdropPath != null) {
            Picasso.get().load(IMAGE_BASE_URL_1280 + backdropPath)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivHorizontalPoster);
        } else {
            // Fallback ke poster jika backdrop null
            String posterPath = getIntent().getStringExtra("poster");
            if (posterPath != null) {
                Picasso.get().load(IMAGE_BASE_URL_1280 + posterPath) // Gunakan URL resolusi tinggi
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivHorizontalPoster);
            }
        }

        // [DIHAPUS] Loading ke ivVerticalPoster sudah tidak diperlukan

        // Format Genres
        List<Genres> labelPS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            labelPS = getIntent().getSerializableExtra("genres", ArrayList.class);
        } else {
            labelPS = (List<Genres>) getIntent().getSerializableExtra("genres");
        }

        if (labelPS != null && !labelPS.isEmpty()) {
            StringBuilder genres = new StringBuilder();
            for (int i = 0; i < labelPS.size(); i++) {
                if (labelPS.get(i) == null) continue;
                genres.append(labelPS.get(i).getName());
                if (i != labelPS.size() - 1) {
                    genres.append(" | ");
                }
            }
            tvGenres.setText(genres.toString());
        } else {
            tvGenres.setText("-");
        }
    }

    private void setupSeasonSpinner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            seasonList = getIntent().getSerializableExtra("season_list", ArrayList.class);
        } else {
            seasonList = (List<Season>) getIntent().getSerializableExtra("season_list");
        }

        if (seasonList != null && !seasonList.isEmpty()) {
            List<String> sNames = new ArrayList<>();
            for (Season s : seasonList) {
                sNames.add(s.getName());
            }

            ArrayAdapter<String> sAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sNames);
            spSeason.setAdapter(sAdapter);

            spSeason.setOnItemClickListener((parent, view1, position, id1) -> {
                Season season = seasonList.get(position);
                loadEpisodes(season.getSeason_number(), true);
            });

            loadEpisodes(seasonList.get(0).getSeason_number(), false);
            spSeason.setText(seasonList.get(0).getName(), false);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void getCastInfo() {
        disposable.add(tmDbAPI.getTvCastDetail(id, TMDb_API_KEY)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    castDataList.addAll(response.getCast());
                    castAdapter.notifyDataSetChanged();
                    if(castDataList.isEmpty()){
                        tvCastTitle.setVisibility(View.GONE);
                    }
                }, e -> Timber.e(e, "Error fetching tv cast: %s", e.getMessage())));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void getRecommendTv() {
        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        String regionCode = prefs.getString("content_region", "ID");

        disposable.add(tmDbAPI.getTvRecommendations(id, TMDb_API_KEY, languageCode, regionCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    recommendDataList.clear();
                    recommendDataList.addAll(response.getResults());
                    recommendAdapter.notifyDataSetChanged();
                    if (recommendDataList.isEmpty()) {
                        tvRelated.setVisibility(View.GONE);
                    }
                }, e -> Timber.e(e, "Error fetching tv recommendations: %s", e.getMessage())));
    }

    private void loadEpisodes(int seasonNumber, boolean showDialog) {
        currentSeasonNumber = seasonNumber;
        disposable.add(tmDbAPI.getSeasonDetail(id, seasonNumber, TMDb_API_KEY, languageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    episodeList = response.getEpisodes();
                    if (episodeList != null) {
                        List<String> eNames = new ArrayList<>();
                        for (Episode e : episodeList) {
                            eNames.add(e.getEpisode_number() + ". " + e.getName());
                        }
                        ArrayAdapter<String> eAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, eNames);
                        spEpisode.setAdapter(eAdapter);
                        spEpisode.setOnItemClickListener((p, v, pos, id2) -> {
                            Episode ep = episodeList.get(pos);
                            loadEpisodeDetail(ep.getEpisode_number());
                        });
                    }
                    if (showDialog) {
                        showSeasonDetail(response);
                    }
                }, e -> Timber.e(e, "Error fetching season detail: %s", e.getMessage())));
    }

    private void loadEpisodeDetail(int episodeNumber) {
        disposable.add(tmDbAPI.getEpisodeDetail(id, currentSeasonNumber, episodeNumber, TMDb_API_KEY, languageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showEpisodeDetail,
                        e -> Timber.e(e, "Error fetching episode detail: %s", e.getMessage())));
    }

    private void showSeasonDetail(ResponseSeasonDetail detail) {
        // Gunakan R.style.Theme_AppCompat_Dialog_Alert atau null agar mengikuti tema gelap
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_season_detail, null);
        // ... (Binding view dialog sama seperti sebelumnya)
        // Kode dialog bisa tetap sama, hanya style context activity yang berubah
        ImageView poster = view.findViewById(R.id.ivSeasonPoster);
        TextView name = view.findViewById(R.id.tvSeasonName);
        TextView overview = view.findViewById(R.id.tvSeasonOverview);

        name.setText(detail.getName());
        overview.setText(detail.getOverview());
        if (detail.getPoster_path() != null) {
            Picasso.get().load("https://image.tmdb.org/t/p/w500" + detail.getPoster_path()).into(poster);
        }
        builder.setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showEpisodeDetail(Episode episode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_episode_detail, null);
        ImageView poster = view.findViewById(R.id.ivEpisodeStill);
        TextView name = view.findViewById(R.id.tvEpisodeName);
        TextView overview = view.findViewById(R.id.tvEpisodeOverview);

        name.setText(episode.getName());
        overview.setText(episode.getOverview());
        if (episode.getStill_path() != null) {
            Picasso.get().load("https://image.tmdb.org/t/p/w500" + episode.getStill_path()).into(poster);
        }
        builder.setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void updateFabStatus() {
        favoritesManager.isFavorite(id)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean isFavorite = task.getResult();
                        fabFavorite.setImageResource(isFavorite ? R.drawable.ic_star_filled_24 : R.drawable.ic_star_border_24);
                    }
                });
    }

    private void handleFavoriteClick() {
        favoritesManager.isFavorite(id)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult()) {
                            favoritesManager.remove(id).addOnSuccessListener(aVoid -> {
                                fabFavorite.setImageResource(R.drawable.ic_star_border_24);
                                Toast.makeText(this, R.string.favorite_removed, Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            Results r = convert(new TvResults(id, title, getIntent().getStringExtra("poster")));
                            favoritesManager.add(r).addOnSuccessListener(aVoid -> {
                                fabFavorite.setImageResource(R.drawable.ic_star_filled_24);
                                Toast.makeText(this, R.string.favorite_added, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
    }

    private Results convert(TvResults tv) {
        Results r = new Results();
        r.setId(tv.getId());
        r.setTitle(tv.getName());
        r.setPoster_path(tv.getPoster_path());
        r.setBackdrop_path(getIntent().getStringExtra("backdrop"));
        r.setOverview(getIntent().getStringExtra("overview"));
        r.setPopularity(getIntent().getDoubleExtra("popularity", 0));
        r.setRelease_date(getIntent().getStringExtra("release_date"));
        return r;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }
}