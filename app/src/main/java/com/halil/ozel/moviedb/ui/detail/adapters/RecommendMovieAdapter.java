package com.halil.ozel.moviedb.ui.detail.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast; // 🆕 Tambahkan Toast

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.halil.ozel.moviedb.App;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.data.Api.TMDbAPI;
import com.halil.ozel.moviedb.data.models.Results;
import com.halil.ozel.moviedb.ui.detail.MovieDetailActivity;
import com.halil.ozel.moviedb.data.FavoritesManager; // FavoritesManager versi Firebase
import com.squareup.picasso.Picasso;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

import static com.halil.ozel.moviedb.data.Api.TMDbAPI.IMAGE_BASE_URL_500;
import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;

import android.content.SharedPreferences;
// import android.content.Context; // Sudah diimpor di atas

public class RecommendMovieAdapter extends RecyclerView.Adapter<RecommendMovieAdapter.RecommendMovieHolder> {

    private String languageCode;
    private final List<Results> popularMovieList;
    private final Context context;
    private final FavoritesManager favoritesManager; // 🆕 DEKLARASI MANAGER NON-STATIS


    @Inject
    TMDbAPI tmDbAPI;

    // 1. ✅ PERBAIKAN KONSTRUKTOR: Menerima 3 Argumen
    public RecommendMovieAdapter(List<Results> popularMovieList, Context context, FavoritesManager manager) {
        this.popularMovieList = popularMovieList;
        this.context = context;
        this.favoritesManager = manager; // 🆕 Simpan instance manager

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        this.languageCode = prefs.getString("language", "en-US");
    }


    @NonNull
    @Override
    public RecommendMovieHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        App.instance().appComponent().inject(this);
        return new RecommendMovieHolder(LayoutInflater.from(context).inflate(R.layout.row_recommend_movie, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull final RecommendMovieHolder holder, final int position) {


        Results results = popularMovieList.get(position);

        holder.tvRecommendMovieTitle.setText(results.getTitle());

        Picasso.get().load(IMAGE_BASE_URL_500 + results.getPoster_path()).into(holder.ivRecommendMoviePoster);

        // --- 2. 🆕 CEK STATUS FAVORIT (ASINKRON) ---
        updateFavoriteIcon(holder.btnFavorite, results.getId());

        // --- 3. 🆕 HANDLER KLIK FAVORIT (ASINKRON) ---
        holder.btnFavorite.setOnClickListener(v -> handleFavoriteClick(holder.btnFavorite, results));


        holder.itemView.setOnClickListener(view -> tmDbAPI.getMovieDetail(results.getId(), TMDb_API_KEY,languageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {

                    Intent intent = new Intent(view.getContext(), MovieDetailActivity.class);
                    // ... (Semua extra tetap sama)
                    intent.putExtra("id", results.getId());
                    intent.putExtra("title", results.getTitle());
                    intent.putExtra("backdrop", results.getBackdrop_path());
                    intent.putExtra("poster", results.getPoster_path());
                    intent.putExtra("overview", results.getOverview());
                    intent.putExtra("popularity", results.getPopularity());
                    intent.putExtra("release_date", results.getRelease_date());
                    intent.putExtra("genres", (Serializable) response.getGenres());
                    view.getContext().startActivity(intent);

                }, e -> Timber.e(e, "Error fetching now popular movies: %s", e.getMessage())));


    }

    // 🆕 Metode Asinkron untuk memperbarui ikon
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

    // 🆕 Metode Asinkron untuk menangani klik
    private void handleFavoriteClick(ImageButton button, Results item) {
        favoritesManager.isFavorite(item.getId())
                .addOnSuccessListener(currentlyFavorite -> {
                    if (currentlyFavorite) {
                        // HAPUS (ASINKRON)
                        favoritesManager.remove(item.getId())
                                .addOnSuccessListener(aVoid -> {
                                    button.setImageResource(R.drawable.ic_star_border_24);
                                    Toast.makeText(context, R.string.favorite_removed, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(context, "Gagal menghapus: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        // TAMBAH (ASINKRON)
                        favoritesManager.add(item)
                                .addOnSuccessListener(aVoid -> {
                                    button.setImageResource(R.drawable.ic_star_filled_24);
                                    Toast.makeText(context, R.string.favorite_added, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(context, "Gagal menambah: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Gagal memuat status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    @Override
    public int getItemCount() {

        return popularMovieList.size();

    }


    public static class RecommendMovieHolder extends RecyclerView.ViewHolder {

        private final TextView tvRecommendMovieTitle;
        private final ImageView ivRecommendMoviePoster;
        private final ImageButton btnFavorite;

        public RecommendMovieHolder(View itemView) {
            super(itemView);
            tvRecommendMovieTitle = itemView.findViewById(R.id.tvRecommendMovieTitle);
            ivRecommendMoviePoster = itemView.findViewById(R.id.ivRecommendMoviePoster);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

    }

}
//package com.halil.ozel.moviedb.ui.detail.adapters;
//
//import android.content.Context;
//import android.content.Intent;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.ImageButton;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.halil.ozel.moviedb.App;
//import com.halil.ozel.moviedb.R;
//import com.halil.ozel.moviedb.data.Api.TMDbAPI;
//import com.halil.ozel.moviedb.data.models.Results;
//import com.halil.ozel.moviedb.ui.detail.MovieDetailActivity;
//import com.halil.ozel.moviedb.data.FavoritesManager;
//import com.squareup.picasso.Picasso;
//
//import java.io.Serializable;
//import java.util.List;
//
//import javax.inject.Inject;
//
//import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
//import io.reactivex.rxjava3.schedulers.Schedulers;
//import timber.log.Timber;
//
//import static com.halil.ozel.moviedb.data.Api.TMDbAPI.IMAGE_BASE_URL_500;
//import static com.halil.ozel.moviedb.data.Api.TMDbAPI.TMDb_API_KEY;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//public class RecommendMovieAdapter extends RecyclerView.Adapter<RecommendMovieAdapter.RecommendMovieHolder> {
//
//    private String languageCode; // <-- DEKLARASI
//    private List<Results> popularMovieList;
//    private Context context;
//
//
//    @Inject
//    TMDbAPI tmDbAPI;
//
//    public RecommendMovieAdapter(List<Results> popularMovieList, Context context) {
//        this.popularMovieList = popularMovieList;
//        this.context = context;
//
//        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
//        this.languageCode = prefs.getString("language", "en-US");
//    }
//
//
//    @NonNull
//    @Override
//    public RecommendMovieHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        App.instance().appComponent().inject(this);
//        return new RecommendMovieHolder(LayoutInflater.from(context).inflate(R.layout.row_recommend_movie, parent, false));
//    }
//
//
//    @Override
//    public void onBindViewHolder(@NonNull final RecommendMovieHolder holder, final int position) {
//
//
//        Results results = popularMovieList.get(position);
//
//        holder.tvRecommendMovieTitle.setText(results.getTitle());
//
//        Picasso.get().load(IMAGE_BASE_URL_500 + results.getPoster_path()).into(holder.ivRecommendMoviePoster);
//
//        boolean fav = FavoritesManager.isFavorite(context, results.getId());
//        holder.btnFavorite.setImageResource(fav ? R.drawable.ic_star_filled_24 : R.drawable.ic_star_border_24);
//
//        holder.btnFavorite.setOnClickListener(v -> {
//            if (FavoritesManager.isFavorite(context, results.getId())) {
//                FavoritesManager.remove(context, results.getId());
//                holder.btnFavorite.setImageResource(R.drawable.ic_star_border_24);
//            } else {
//                FavoritesManager.add(context, results);
//                holder.btnFavorite.setImageResource(R.drawable.ic_star_filled_24);
//            }
//        });
//
//
//        holder.itemView.setOnClickListener(view -> tmDbAPI.getMovieDetail(results.getId(), TMDb_API_KEY,languageCode)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(response -> {
//
//                    Intent intent = new Intent(view.getContext(), MovieDetailActivity.class);
//                    intent.putExtra("id", results.getId());
//                    intent.putExtra("title", results.getTitle());
//                    intent.putExtra("backdrop", results.getBackdrop_path());
//                    intent.putExtra("poster", results.getPoster_path());
//                    intent.putExtra("overview", results.getOverview());
//                    intent.putExtra("popularity", results.getPopularity());
//                    intent.putExtra("release_date", results.getRelease_date());
//                    intent.putExtra("genres", (Serializable) response.getGenres());
//                    view.getContext().startActivity(intent);
//
//                }, e -> Timber.e(e, "Error fetching now popular movies: %s", e.getMessage())));
//
//
//    }
//
//
//    @Override
//    public int getItemCount() {
//
//        return popularMovieList.size();
//
//    }
//
//
//    public static class RecommendMovieHolder extends RecyclerView.ViewHolder {
//
//        private final TextView tvRecommendMovieTitle;
//        private final ImageView ivRecommendMoviePoster;
//        private final ImageButton btnFavorite;
//
//        public RecommendMovieHolder(View itemView) {
//            super(itemView);
//            tvRecommendMovieTitle = itemView.findViewById(R.id.tvRecommendMovieTitle);
//            ivRecommendMoviePoster = itemView.findViewById(R.id.ivRecommendMoviePoster);
//            btnFavorite = itemView.findViewById(R.id.btnFavorite);
//        }
//
//    }
//
//}
