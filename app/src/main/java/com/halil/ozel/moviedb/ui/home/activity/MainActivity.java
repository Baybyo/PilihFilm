package com.halil.ozel.moviedb.ui.home.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.halil.ozel.moviedb.App;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.ui.auth.LoginActivity;
import com.halil.ozel.moviedb.ui.home.fragments.FavoriteFragment;
import com.halil.ozel.moviedb.ui.home.fragments.MoviesFragment;
import com.halil.ozel.moviedb.ui.home.fragments.SearchFragment;
import com.halil.ozel.moviedb.ui.home.fragments.SettingsFragment;
import com.halil.ozel.moviedb.ui.home.fragments.TvSeriesFragment;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Setup Bahasa (Locale) sebelum setContentView
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String lang = prefs.getString("language", "en");
        App.instance().applyLocale(lang);

        // 2. Inject Dependency (Dagger)
        App.instance().appComponent().inject(this);

        // 3. Setup Layout
        setContentView(R.layout.activity_main);

        // 4. Setup Auth (Firebase & Google)
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 5. Setup Navigasi Bawah (Bottom Navigation)
        BottomNavigationView nav = findViewById(R.id.nav_view);

        nav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            // Menggunakan IF-ELSE agar kompatibel dengan semua versi Gradle
            if (id == R.id.navigation_tv) {
                selectedFragment = new TvSeriesFragment();
            } else if (id == R.id.navigation_favorite) {
                selectedFragment = new FavoriteFragment();
            } else if (id == R.id.navigation_search) {
                selectedFragment = new SearchFragment();
            } else if (id == R.id.navigation_settings) {
                selectedFragment = new SettingsFragment();
            } else {
                // Default: Home / Movies
                selectedFragment = new MoviesFragment();
            }

            // Ganti Fragment
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // 6. Load Fragment Default saat aplikasi pertama dibuka (cegah duplikasi jika savedInstanceState null)
        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.navigation_home);
        }
    }

    // Method Logout yang bisa dipanggil dari SettingsFragment
    // Cara panggil: ((MainActivity) getActivity()).logout();
    public void logout() {
        // Logout dari Firebase
        mAuth.signOut();

        // Logout dari Google Client
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            // Clear Task agar user tidak bisa kembali ke Main dengan tombol Back
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}