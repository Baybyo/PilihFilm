package com.halil.ozel.moviedb.ui.splash;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper; // [PERBAIKAN 1] Tambahkan import Looper
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.ui.auth.LoginActivity;
import com.halil.ozel.moviedb.ui.home.activity.MainActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // --- 1. MULAI ANIMASI GLOW ---
        // Inisialisasi View (Sesuai ID di XML yang kamu perbaiki sebelumnya: @+id/img_glow)
        ImageView imgGlow = findViewById(R.id.img_logo_main);

        // Load Animasi Pulse dari folder res/anim/pulse_glow.xml
        Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_glow);

        // Jalankan animasi pada logo glow
        imgGlow.startAnimation(pulseAnimation);
        // -----------------------------

        // --- 2. LOGIKA AUTHENTICATION ---
        mAuth = FirebaseAuth.getInstance();
        final FirebaseUser currentUser = mAuth.getCurrentUser();

        // --- 3. HANDLER PINDAH HALAMAN ---
        // [PERBAIKAN 2] Menggunakan Looper.getMainLooper() untuk thread safety
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Logika asli: Cek status login & verifikasi email
                if (currentUser != null && currentUser.isEmailVerified()) {
                    // User sudah login dan terverifikasi -> Ke MainActivity
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } else {
                    // User belum login atau belum verifikasi -> Ke LoginActivity
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }

                finish();
            }
        }, 3000); // Durasi 3 detik
    }
}