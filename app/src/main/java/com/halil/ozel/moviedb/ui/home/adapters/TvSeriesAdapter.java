package com.halil.ozel.moviedb.ui.home.adapters;

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
import com.halil.ozel.moviedb.data.models.ResponseTvSeriesDetail;
import com.halil.ozel.moviedb.data.models.Results;
import com.halil.ozel.moviedb.data.models.TvResults;
import com.halil.ozel.moviedb.ui.detail.TvSeriesDetailActivity;
import com.squareup.picasso.Picasso;

import java.util.List;

import javax.inject.Inject;

import static com.halil.ozel.moviedb.data.Api.TMDbAPI.IMAGE_BASE_URL_500;
import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class TvSeriesAdapter extends RecyclerView.Adapter<TvSeriesAdapter.TvSeriesHolder> {
    private String languageCode;
    private final List<TvResults> tvList;
    private final Context context;
    private final FavoritesManager favoritesManager;

    @Inject
    TMDbAPI tmDbAPI;

    public TvSeriesAdapter(List<TvResults> tvList, Context context, FavoritesManager manager) {
        this.tvList = tvList;
        this.context = context;
        this.favoritesManager = manager;

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        this.languageCode = prefs.getString("language", "en-US");
    }

    @NonNull
    @Override
    public TvSeriesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        App.instance().appComponent().inject(this);
        // Menggunakan layout row_tvseries.xml
        return new TvSeriesHolder(LayoutInflater.from(context).inflate(R.layout.row_tvseries, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TvSeriesHolder holder, int position) {
        TvResults tv = tvList.get(position);

        // 1. Set Judul
        holder.title.setText(tv.getName());

        // 2. Set Tanggal Rilis (Tambahan Baru)
        if (tv.getFirst_air_date() != null && !tv.getFirst_air_date().isEmpty()) {
            holder.date.setText(tv.getFirst_air_date());
        } else {
            holder.date.setText("-");
        }

        // 3. Set Rating (Tambahan Baru)
        // String.valueOf mengubah double/float menjadi string
        holder.rating.setText(String.valueOf(tv.getVote_average()));

        // 4. Set Poster
        if (tv.getPoster_path() != null) {
            Picasso.get()
                    .load(IMAGE_BASE_URL_500 + tv.getPoster_path())
                    .into(holder.poster);
        } else {
            Picasso.get()
                    .load("https://www.salonlfc.com/wp-content/uploads/2018/01/image-not-found-scaled-1150x647.png")
                    .into(holder.poster);
        }

        // 5. Cek Status Favorit
        updateFavoriteIcon(holder.btnFavorite, tv.getId());

        // 6. Handle Klik Favorit
        holder.btnFavorite.setOnClickListener(v -> handleFavoriteClick(holder.btnFavorite, tv));

        // 7. Handle Klik Item (Detail)
        holder.itemView.setOnClickListener(view -> tmDbAPI.getTvSeriesDetail(tv.getId(), TMDb_API_KEY, languageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    Intent intent = new Intent(view.getContext(), TvSeriesDetailActivity.class);
                    intent.putExtra("id", tv.getId());
                    intent.putExtra("title", tv.getName());
                    intent.putExtra("poster", tv.getPoster_path());
                    intent.putExtra("overview", response instanceof ResponseTvSeriesDetail ? ((ResponseTvSeriesDetail) response).getOverview() : "");
                    intent.putExtra("popularity", response.getPopularity());
                    intent.putExtra("release_date", response.getFirst_air_date());
                    intent.putExtra("seasons", response.getNumber_of_seasons());
                    intent.putExtra("episodes", response.getNumber_of_episodes());
                    intent.putExtra("season_list", (java.io.Serializable) response.getSeasons());
                    intent.putExtra("genres", (java.io.Serializable) response.getGenres());
                    view.getContext().startActivity(intent);
                }, e -> Timber.e(e, "Error fetching tv detail: %s", e.getMessage())));
    }

    private void updateFavoriteIcon(ImageButton button, int id) {
        favoritesManager.isFavorite(id)
                .addOnSuccessListener(isFav -> {
                    button.setImageResource(isFav ? R.drawable.ic_star_filled_24 : R.drawable.ic_star_border_24);
                })
                .addOnFailureListener(e -> {
                    Timber.e(e, "Gagal cek status favorit di adapter: %s", e.getMessage());
                    button.setImageResource(R.drawable.ic_star_border_24);
                });
    }

    private void handleFavoriteClick(ImageButton button, TvResults tvItem) {
        favoritesManager.isFavorite(tvItem.getId())
                .addOnSuccessListener(currentlyFavorite -> {
                    if (currentlyFavorite) {
                        favoritesManager.remove(tvItem.getId())
                                .addOnSuccessListener(aVoid -> {
                                    button.setImageResource(R.drawable.ic_star_border_24);
                                    Toast.makeText(context, R.string.favorite_removed, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(context, "Gagal menghapus: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Results r = convert(tvItem);
                        favoritesManager.add(r)
                                .addOnSuccessListener(aVoid -> {
                                    button.setImageResource(R.drawable.ic_star_filled_24);
                                    Toast.makeText(context, R.string.favorite_added, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(context, "Gagal menambah: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Gagal memuat status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private Results convert(TvResults tv) {
        Results r = new Results();
        r.setId(tv.getId());
        r.setTitle(tv.getName());
        r.setPoster_path(tv.getPoster_path());
        return r;
    }

    @Override
    public int getItemCount() {
        return tvList.size();
    }

    // --- BAGIAN INI YANG DIPERBAIKI (ViewHolder) ---
    static class TvSeriesHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView poster;
        ImageButton btnFavorite;
        // Tambahan variable baru
        TextView date;
        TextView rating;

        TvSeriesHolder(View itemView) {
            super(itemView);
            // MENGGUNAKAN ID BARU DARI row_tvseries.xml
            title = itemView.findViewById(R.id.tvTitle);      // Sebelumnya tvPopularMovieTitle
            poster = itemView.findViewById(R.id.imgPoster);   // Sebelumnya ivPopularPoster
            btnFavorite = itemView.findViewById(R.id.btnFavorite);

            // Mengambil ID untuk Tanggal dan Rating
            date = itemView.findViewById(R.id.tvDate);
            rating = itemView.findViewById(R.id.tvRating);
        }
    }
}