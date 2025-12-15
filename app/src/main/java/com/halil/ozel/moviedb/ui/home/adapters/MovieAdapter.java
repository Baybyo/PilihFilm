package com.halil.ozel.moviedb.ui.home.adapters;

import static com.halil.ozel.moviedb.data.Api.TMDbAPI.IMAGE_BASE_URL_500;
import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.halil.ozel.moviedb.App;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.data.Api.TMDbAPI;
import com.halil.ozel.moviedb.data.FavoritesManager;
import com.halil.ozel.moviedb.data.models.Results;
import com.halil.ozel.moviedb.ui.detail.MovieDetailActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.PopularMovieHolder> {
    private String languageCode;
    private final List<Results> popularMovieList;
    private final Context context;
    private final FavoritesManager favoritesManager;
    private OnFavoriteChangeListener favoriteChangeListener;

    @Inject
    TMDbAPI tmDbAPI;

    public MovieAdapter(List<Results> popularMovieList, Context context, FavoritesManager manager) {
        this.popularMovieList = popularMovieList;
        this.context = context;
        this.favoritesManager = manager;
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        this.languageCode = prefs.getString("language", "en-US");
    }

    public interface OnFavoriteChangeListener {
        void onChange();
    }

    public void setOnFavoriteChangeListener(OnFavoriteChangeListener listener) {
        this.favoriteChangeListener = listener;
    }

    @NonNull
    @Override
    public PopularMovieHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        App.instance().appComponent().inject(this);

        // --- PERUBAHAN PENTING DISINI ---
        // Kita menggunakan item_movie.xml sekarang
        return new PopularMovieHolder(LayoutInflater.from(context).inflate(R.layout.item_movie, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final PopularMovieHolder holder, final int position) {
        Results results = popularMovieList.get(position);

        // Set Text Data
        holder.tvPopularMovieTitle.setText(results.getTitle());

        if (results.getRelease_date() != null) {
            holder.tvDate.setText(results.getRelease_date());
        }

        holder.tvRating.setText(results.getVote_average() != null ? String.valueOf(results.getVote_average()) : "N/A");

        // Load Image
        String imagePath = results.getPoster_path();
        if (imagePath != null && !imagePath.isEmpty()) {
            Glide.with(context)
                    .load(IMAGE_BASE_URL_500 + imagePath)
                    .apply(new RequestOptions()
                            .placeholder(R.color.colorPrimaryDark)
                            .error(android.R.color.darker_gray)
                            .transform(new CenterCrop(), new RoundedCorners(32)))
                    .into(holder.ivPopularPoster);
        } else {
            holder.ivPopularPoster.setImageResource(R.drawable.ic_launcher_background);
        }

        // Logic Favorite
        if (favoritesManager != null) {
            updateFavoriteIcon(holder.btnFavorite, results.getId());
            holder.btnFavorite.setOnClickListener(v -> handleFavoriteClick(holder.btnFavorite, results));
        }

        // Logic Klik Pindah ke Detail
        holder.itemView.setOnClickListener(view -> {
            tmDbAPI.getMovieDetail(results.getId(), TMDb_API_KEY, languageCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(response -> {
                        Intent intent = new Intent(context, MovieDetailActivity.class);
                        intent.putExtra("id", results.getId());
                        intent.putExtra("title", results.getTitle());
                        intent.putExtra("backdrop", results.getBackdrop_path());
                        intent.putExtra("poster", results.getPoster_path());
                        intent.putExtra("popularity", results.getPopularity());
                        intent.putExtra("release_date", results.getRelease_date());

                        String overview = "";
                        if (response != null && response.getOverview() != null) overview = response.getOverview();
                        intent.putExtra("overview", overview);

                        if (response != null && response.getGenres() != null) {
                            intent.putExtra("genres", new ArrayList<>(response.getGenres()));
                        }

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }, e -> {
                        Timber.e(e, "Gagal mengambil detail movie");
                        Toast.makeText(context, "Gagal memuat detail.", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void updateFavoriteIcon(ImageButton button, int id) {
        favoritesManager.isFavorite(id)
                .addOnSuccessListener(isFav -> {
                    button.setImageResource(isFav ? R.drawable.ic_star_filled_24 : R.drawable.ic_star_border_24);
                })
                .addOnFailureListener(e -> button.setImageResource(R.drawable.ic_star_border_24));
    }

    private void handleFavoriteClick(ImageButton button, Results item) {
        favoritesManager.isFavorite(item.getId())
                .addOnSuccessListener(currentlyFavorite -> {
                    if (currentlyFavorite) {
                        favoritesManager.remove(item.getId()).addOnSuccessListener(aVoid -> {
                            button.setImageResource(R.drawable.ic_star_border_24);
                            Toast.makeText(context, R.string.favorite_removed, Toast.LENGTH_SHORT).show();
                            if (favoriteChangeListener != null) favoriteChangeListener.onChange();
                        });
                    } else {
                        favoritesManager.add(item).addOnSuccessListener(aVoid -> {
                            button.setImageResource(R.drawable.ic_star_filled_24);
                            Toast.makeText(context, R.string.favorite_added, Toast.LENGTH_SHORT).show();
                            if (favoriteChangeListener != null) favoriteChangeListener.onChange();
                        });
                    }
                });
    }

    @Override
    public int getItemCount() {
        return popularMovieList.size();
    }

    // --- HOLDER DIPERBAIKI UNTUK COCOK DENGAN ITEM_MOVIE.XML ---
    public static class PopularMovieHolder extends RecyclerView.ViewHolder {
        TextView tvPopularMovieTitle;
        TextView tvRating;
        TextView tvDate;
        ImageView ivPopularPoster;
        ImageButton btnFavorite;

        public PopularMovieHolder(View itemView) {
            super(itemView);
            // Mapping ID sesuai item_movie.xml
            tvPopularMovieTitle = itemView.findViewById(R.id.tvTitle);
            ivPopularPoster = itemView.findViewById(R.id.imgPoster);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}