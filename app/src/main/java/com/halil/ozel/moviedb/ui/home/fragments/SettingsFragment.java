package com.halil.ozel.moviedb.ui.home.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout; // Import untuk btn_language
import android.widget.Spinner;
import android.widget.Button;
import android.app.AlertDialog;
import android.widget.Toast;
import java.io.File;
import android.content.Context;

// --- IMPORT UNTUK VERSI APLIKASI ---
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.TextView;
// ------------------------------------

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.halil.ozel.moviedb.App;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.ui.home.activity.MainActivity;

import java.util.Locale; // Digunakan di metode updateLanguageUI

public class SettingsFragment extends Fragment {

    private static final String PREFS = "settings";
    private static final String KEY_DARK = "dark";
    private static final String KEY_LANG = "language";
    private static final String KEY_ADULT = "include_adult";
    private static final String KEY_REGION = "content_region";
    private static final String SEARCH_HISTORY_PREFS = "search_history_prefs";
    private static final String KEY_HISTORY_SET = "search_history_set";
    private static final String TAG = "SettingsFragment";

    // --- DEKLARASI UI DISESUAIKAN DENGAN XML ---
    private Button btnLogout, btnProfile, btnClearCache, btnAbout, btnClearWishlist, btnClearSearchHistory;
    private SwitchCompat switchSafeSearch;
    private SwitchCompat switchTheme;
    private TextView tvVersion;

    // UI BARU DARI XML GABUNGAN: Untuk menggantikan Spinner Bahasa
    private LinearLayout btnLanguage; // LinearLayout yang berfungsi sebagai tombol
    private TextView tvCurrentLang; // TextView untuk menampilkan bahasa saat ini

    // Spinner Region dipertahankan
    private Spinner spinnerRegion;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;
    private String[] regionCodes;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Inisialisasi Firebase & Prefs
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        regionCodes = getResources().getStringArray(R.array.region_codes);

        // --- Bagian Kategori Tampilan (Language & Theme) ---
        // Ganti logic setupLanguageSpinner dengan LinearLayout click listener
        btnLanguage = view.findViewById(R.id.btnLanguage); // Asumsi ID baru: btnLanguage (diperbaiki dari btn_language)
        tvCurrentLang = view.findViewById(R.id.tv_current_lang);
        setupLanguageClickListener(view);

        // NOTE: Asumsi R.id.switchTheme ada di layout
        switchTheme = view.findViewById(R.id.switchTheme);
        setupThemeSwitch(view);

        // Setup Region Spinner tetap
//        setupRegionSpinner(view);

        // --- Bagian Kategori Akun ---
        btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        btnProfile = view.findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getActivity(), com.halil.ozel.moviedb.ui.auth.ProfileActivity.class);
            startActivity(intent);
        });

        // --- Bagian Kategori Info & Bantuan ---
        btnClearCache = view.findViewById(R.id.btnClearCache);
        btnClearCache.setOnClickListener(v -> clearAppCache());

        btnAbout = view.findViewById(R.id.btnAbout);
        btnAbout.setOnClickListener(v -> showAboutDialog());

        // --- INISIALISASI DAN SETUP VERSION INFO ---
        tvVersion = view.findViewById(R.id.tv_app_version);
        setupVersionInfo();
        // ---------------------------------------------------------

        // --- KUSTOMISASI KONTEN (Mode Aman) ---
        setupSafeSearchSwitch(view);

        // --- Bagian Kategori Manajemen Data ---
        btnClearWishlist = view.findViewById(R.id.btnClearWishlist);
        btnClearWishlist.setOnClickListener(v -> showClearWishlistConfirmation());

        btnClearSearchHistory = view.findViewById(R.id.btnClearSearchHistory);
        btnClearSearchHistory.setOnClickListener(v -> showClearSearchHistoryConfirmation());

        // Update UI awal untuk Bahasa dan Safe Search
        updateLanguageUI();

        return view;
    }

    // --- METODE BARU: LOGIKA GANTI BAHASA DENGAN DIALOG (Menggantikan Spinner) ---
    private void setupLanguageClickListener(View view) {
        // Asumsi: btnLanguage adalah LinearLayout atau View yang dapat diklik
        btnLanguage = view.findViewById(R.id.btnLanguage);
        btnLanguage.setOnClickListener(v -> showLanguageDialog());
    }

    private void showLanguageDialog() {
        final String[] displayNames = getResources().getStringArray(R.array.language_names); // Asumsi Anda memiliki array nama yang mudah dibaca
        final String[] codes = getResources().getStringArray(R.array.language_values);

        // Cari pilihan yang sedang aktif
        String currentCode = prefs.getString(KEY_LANG, "en-US");
        int checkedItem = -1;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentCode)) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.select_language);
        builder.setSingleChoiceItems(displayNames, checkedItem, (dialog, which) -> {
            String selectedCode = codes[which];
            if (!selectedCode.equals(prefs.getString(KEY_LANG, "en-US"))) {
                prefs.edit().putString(KEY_LANG, selectedCode).apply();

                // Update UI di fragment tanpa restart
                updateLanguageUI();

                // Menerapkan perubahan lokal (memerlukan restart activity)
                App.instance().applyLocale(selectedCode);

                Toast.makeText(getContext(), "Bahasa diubah ke " + displayNames[which], Toast.LENGTH_SHORT).show();

                // Meminta user untuk restart atau langsung restart
                requireActivity().recreate();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void updateLanguageUI() {
        if (tvCurrentLang == null) return;

        String code = prefs.getString(KEY_LANG, "en-US");
        // Mencari nama lengkap bahasa (Opsional: jika resource array codes dan names cocok)
        String[] displayNames = getResources().getStringArray(R.array.language_names);
        String[] codes = getResources().getStringArray(R.array.language_values);

        String currentLangName = "English (US)"; // Default
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(code)) {
                currentLangName = displayNames[i];
                break;
            }
        }

        tvCurrentLang.setText(currentLangName);
    }
    // --------------------------------------------------------------------------


    private void setupVersionInfo() {
        try {
            if (getActivity() != null) {
                PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                tvVersion.setText("v" + pInfo.versionName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            tvVersion.setText("v1.0");
        } catch (NullPointerException e) {
            Log.e(TAG, "TextView version not found in layout");
        }
    }

    private void setupThemeSwitch(View view) {
        switchTheme = view.findViewById(R.id.switchTheme);
        boolean dark = prefs.getBoolean(KEY_DARK, false);
        switchTheme.setChecked(dark);
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_DARK, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
            requireActivity().recreate();
        });
    }

    private void setupSafeSearchSwitch(View view) {
        switchSafeSearch = view.findViewById(R.id.switch_safe_search);

        // Load nilai 'include_adult'
        boolean includeAdult = prefs.getBoolean(KEY_ADULT, false);

        // Logika dibalik: Jika includeAdult=false (SafeMode ON), maka switch harus checked.
        boolean isSafeMode = !includeAdult;
        switchSafeSearch.setChecked(isSafeMode);

        switchSafeSearch.setOnCheckedChangeListener((buttonView, isChecked) -> {

            // Logika dibalik: Jika checked=true (Mode Aman ON), maka include_adult=false
            boolean setIncludeAdult = !isChecked;
            prefs.edit().putBoolean(KEY_ADULT, setIncludeAdult).apply();

            if (isChecked) {
                Toast.makeText(getContext(), "Mode Aman Aktif: Konten dewasa disembunyikan", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Peringatan: Konten dewasa mungkin muncul", Toast.LENGTH_SHORT).show();
            }
        });
    }

//    private void setupRegionSpinner(View view) {
//        spinnerRegion = view.findViewById(R.id.spinnerRegion);
//        String currentRegionCode = prefs.getString(KEY_REGION, "ID");
//        int initialSelection = 0;
//        for (int i = 0; i < regionCodes.length; i++) {
//            if (regionCodes[i].equals(currentRegionCode)) {
//                initialSelection = i;
//                break;
//            }
//        }
//        spinnerRegion.setSelection(initialSelection);
//
//        spinnerRegion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
//                String selectedCode = regionCodes[position];
//                if (!selectedCode.equals(prefs.getString(KEY_REGION, "ID"))) {
//                    prefs.edit().putString(KEY_REGION, selectedCode).apply();
//                    requireActivity().recreate();
//                }
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) { }
//        });
//    }


    private void showLogoutConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).logout();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void clearAppCache() {
        try {
            File cacheDir = getContext().getCacheDir();
            boolean success = deleteDir(cacheDir);
            if (success) {
                Toast.makeText(getContext(), R.string.clear_cache_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), R.string.clear_cache_fail, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.clear_cache_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private void clearUserWishlist() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), R.string.clear_wishlist_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(getContext(), R.string.clear_wishlist_success, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), R.string.clear_wishlist_success, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Semua Fsvorit berhasil dihapus.");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), R.string.clear_wishlist_fail, Toast.LENGTH_SHORT).show();
                                Log.w(TAG, "Gagal menjalankan batch delete wishlist", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), R.string.clear_wishlist_fail, Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Gagal mengambil data wishlist untuk dihapus", e);
                });
    }

    private void showClearWishlistConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.clear_wishlist_title)
                .setMessage(R.string.clear_wishlist_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    clearUserWishlist();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }


    private void showClearSearchHistoryConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.clear_search_history_title)
                .setMessage(R.string.clear_search_history_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    clearSearchHistory();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void clearSearchHistory() {
        SharedPreferences searchPrefs = getContext().getSharedPreferences(SEARCH_HISTORY_PREFS, Context.MODE_PRIVATE);
        searchPrefs.edit().remove(KEY_HISTORY_SET).apply();
        Toast.makeText(getContext(), R.string.clear_search_history_success, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Riwayat pencarian dihapus.");
    }
}