package com.halil.ozel.moviedb.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.halil.ozel.moviedb.R;
import com.halil.ozel.moviedb.ui.home.activity.MainActivity; // Pastikan path ini benar
import com.google.firebase.firestore.DocumentReference; // IMPORT BARU
import com.google.firebase.firestore.DocumentSnapshot; // IMPORT BARU
import com.google.firebase.firestore.FirebaseFirestore; // IMPORT BARU
import java.util.HashMap; // IMPORT BARU
import java.util.Map; // IMPORT BARU

public class LoginActivity extends AppCompatActivity {

    private EditText etEmailUsername, etPassword;
    private Button btnLogin, btnGoogleSignIn;
    private TextView tvRegister, tvForgotPassword;
    // private ProgressBar progressBar; // Opsional

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db; // DEKLARASI BARU

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // INISIALISASI BARU

        etEmailUsername = findViewById(R.id.etEmailUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        // progressBar = findViewById(R.id.progressBar); // Opsional

        // --- Konfigurasi Google Sign-In ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Ambil ID dari strings.xml
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // --- Akhir Konfigurasi Google ---

        // Listener untuk Login Manual
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginManual();
            }
        });

        // Listener untuk teks Register
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        // Listener untuk teks Lupa Password
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            }
        });

        // Listener untuk Google Sign-In
        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });
    }

    private void loginManual() {
        // (Kode loginManual Anda dari sebelumnya masih sama)
        String email = etEmailUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmailUsername.setError("Email tidak boleh kosong");
            etEmailUsername.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password tidak boleh kosong");
            etPassword.requestFocus();
            return;
        }

        // progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                updateUI(user);
                            } else {
                                mAuth.signOut();
                                Toast.makeText(LoginActivity.this, "Login Gagal. Silakan verifikasi email Anda terlebih dahulu.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Login Gagal: Email atau password salah.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // --- METODE BARU UNTUK GOOGLE SIGN-IN ---

    private void signInWithGoogle() {
        // progressBar.setVisibility(View.VISIBLE);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Hasil kembali dari peluncuran Intent GoogleSignInClient.getSignInIntent()
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In berhasil, autentikasi dengan Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In gagal
                // progressBar.setVisibility(View.GONE);
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // --- BAGIAN BARU: Cek jika user baru (dari Google) ---
                            checkAndSaveGoogleUser(user);
                            // --- AKHIR BAGIAN BARU ---
                        } else {
                            // progressBar.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this, "Autentikasi Gagal.", Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    } else {
                        // progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Autentikasi Firebase Gagal.", Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }
    // --- METODE BARU ---
    private void checkAndSaveGoogleUser(FirebaseUser user) {
        String userId = user.getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    // Pengguna baru -> simpan datanya
                    Log.d(TAG, "Pengguna Google baru. Menyimpan ke Firestore.");
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("username", user.getDisplayName()); // Ambil nama dari Google
                    userMap.put("email", user.getEmail());
                    userMap.put("profileImageUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);

                    userRef.set(userMap)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Data user Google berhasil disimpan.");
                                updateUI(user); // Lanjut ke Main
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error menyimpan data user Google", e);
                                updateUI(user); // Tetap lanjut ke Main
                            });
                } else {
                    // Pengguna lama -> datanya sudah ada
                    Log.d(TAG, "Pengguna Google lama. Langsung login.");
                    // progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Login Google Berhasil.", Toast.LENGTH_SHORT).show();
                    updateUI(user); // Lanjut ke Main
                }
            } else {
                Log.w(TAG, "Gagal mengecek data user", task.getException());
                // progressBar.setVisibility(View.GONE);
                updateUI(user); // Gagal cek, tapi tetap login
            }
        });
    }

    // Metode helper untuk pindah activity
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // Pindah ke MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}