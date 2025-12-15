package com.halil.ozel.moviedb.ui.home.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // 🆕 Import TextView
import android.widget.Toast; // 🆕 Import Toast

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.data.FavoritesManager; // FavoritesManager versi Firebase
import com.halil.ozel.moviedb.data.models.Results;
import com.halil.ozel.moviedb.ui.home.adapters.MovieAdapter;
import com.google.firebase.auth.FirebaseAuth; // 🆕 Import FirebaseAuth

import java.util.ArrayList; // 🆕 Import ArrayList
import java.util.List;

public class FavoriteFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovieAdapter adapter;
    private FavoritesManager favoritesManager; // 🆕 Deklarasi Manager Non-statis
    private List<Results> favoriteList; // 🆕 Deklarasi List
    private TextView tvEmptyMessage; // 🆕 Tambahkan TextView untuk pesan kosong

    // (Tambahkan ProgressBar jika ada di layout Anda)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);

        // 🆕 Inisialisasi Manager dan List
        favoritesManager = new FavoritesManager();
        favoriteList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.rvFavorites);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage); // Asumsi Anda punya TextView ini di layout

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setNestedScrollingEnabled(false);

        // 🆕 Inisialisasi Adapter dengan 3 argumen
        adapter = new MovieAdapter(favoriteList, requireContext(), favoritesManager);
        // Atur listener agar data di-refresh setelah item di-toggle (dihapus/ditambah)
        adapter.setOnFavoriteChangeListener(this::loadData);
        recyclerView.setAdapter(adapter);

        // Panggil loadData di onCreateView
        loadData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Hanya perlu memuat ulang data saat Fragment dilanjutkan
        loadData();
    }

    // 🆕 Metode loadData() ASINKRON
    private void loadData() {
        // Cek dulu apakah user sudah login
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Tampilkan pesan: "Silakan login untuk melihat favorit"
            Toast.makeText(getContext(), "Silakan login untuk melihat favorit.", Toast.LENGTH_LONG).show();
            favoriteList.clear();
            adapter.notifyDataSetChanged();
            // Tampilkan pesan kosong/login prompt jika perlu
            return;
        }

        // (Opsional: Tampilkan ProgressBar)

        favoritesManager.load() // Mengembalikan Task<List<Results>>
                .addOnSuccessListener(list -> {
                    // (Sembunyikan ProgressBar)

                    favoriteList.clear();
                    favoriteList.addAll(list);
                    adapter.notifyDataSetChanged(); // Perbarui tampilan

                    // Logika tampilan pesan kosong
                    if (list.isEmpty()) {
                        tvEmptyMessage.setVisibility(View.VISIBLE);
                    } else {
                        tvEmptyMessage.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    // (Sembunyikan ProgressBar)
                    Toast.makeText(getContext(), "Gagal memuat favorit: " + e.getMessage(), Toast.LENGTH_LONG).show();

                    // Jika gagal, kosongkan daftar
                    favoriteList.clear();
                    adapter.notifyDataSetChanged();
                    tvEmptyMessage.setVisibility(View.VISIBLE);
                });
    }
}
//package com.halil.ozel.moviedb.ui.home.fragments;
//
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.halil.ozel.moviedb.R;
//import com.halil.ozel.moviedb.data.FavoritesManager;
//import com.halil.ozel.moviedb.data.models.Results;
//import com.halil.ozel.moviedb.ui.home.adapters.MovieAdapter;
//
//import java.util.List;
//
//public class FavoriteFragment extends Fragment {
//
//    private RecyclerView recyclerView;
//    private MovieAdapter adapter;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_favorite, container, false);
//        recyclerView = view.findViewById(R.id.rvFavorites);
//        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
//        recyclerView.setNestedScrollingEnabled(false);
//
//        loadData();
//        return view;
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        loadData();
//    }
//
//    private void loadData() {
//        List<Results> list = FavoritesManager.load(requireContext());
//        adapter = new MovieAdapter(list, getContext());
//        adapter.setOnFavoriteChangeListener(this::loadData);
//        recyclerView.setAdapter(adapter);
//    }
//}
