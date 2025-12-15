package com.halil.ozel.moviedb.ui.auth;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.halil.ozel.moviedb.R;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ProgressBar progressBar, pbUploadPhoto;
    private EditText etUsername, etEmail;
    private TextView tvChangePassword, tvDeleteAccount;
    private Button btnEditSaveProfile;
    private CircleImageView ivProfilePhoto;
    private FloatingActionButton fabEditPhoto;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private DocumentReference userRef;

    private boolean isEditMode = false;
    private static final String TAG = "ProfileActivity";

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Inisialisasi Cloudinary (Hanya sekali)
        initCloudinary();

        // 2. Inisialisasi Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // 3. Hubungkan Views
        toolbar = findViewById(R.id.toolbarProfile);
        progressBar = findViewById(R.id.progressBarProfile);
        pbUploadPhoto = findViewById(R.id.pbUploadPhoto);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        tvChangePassword = findViewById(R.id.tvChangePassword);
        tvDeleteAccount = findViewById(R.id.tvDeleteAccount);
        btnEditSaveProfile = findViewById(R.id.btnEditSaveProfile);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        fabEditPhoto = findViewById(R.id.fabEditPhoto);

        // 4. Setup Launcher dan Toolbar
        setupResultLaunchers();
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // 5. Listener Tombol
        btnEditSaveProfile.setOnClickListener(v -> handleEditSaveClick());
        tvChangePassword.setOnClickListener(v -> handleChangePasswordClick());
        tvDeleteAccount.setOnClickListener(v -> showDeleteAccountWarning());
        fabEditPhoto.setOnClickListener(v -> checkStoragePermission());

        // 6. Muat Data
        loadUserProfile();
    }

    private void initCloudinary() {
        try {
            // GANTI DENGAN KREDENSIAL CLOUDINARY ANDA
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dnedqu9ns"); // Ganti ini
            config.put("api_key", "794972286779741");       // Ganti ini
            config.put("api_secret", "OzhRg9rzZqePdPKsiUB59ePF0tQ"); // Ganti ini
            MediaManager.init(this, config);
        } catch (Exception e) {
            // Sudah terinisialisasi, abaikan error
        }
    }

    private void setupResultLaunchers() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) openImagePicker();
                    else Toast.makeText(this, "Izin penyimpanan ditolak", Toast.LENGTH_LONG).show();
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            ivProfilePhoto.setImageURI(imageUri); // Tampilkan sementara
                            uploadImageToCloudinary(imageUri);    // Upload
                        }
                    }
                }
        );
    }

    private void checkStoragePermission() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        if (currentUser == null) return;
        showImageLoading(true);

        String requestId = MediaManager.get().upload(imageUri)
                .option("folder", "user_profiles") // Folder di Cloudinary
                .option("public_id", currentUser.getUid()) // Nama file = UID User (biar menimpa file lama)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Sukses upload, dapatkan URL gambar (Secure URL lebih aman)
                        String downloadUrl = (String) resultData.get("secure_url");
                        updatePhotoUrlInFirestore(downloadUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        showImageLoading(false);
                        Toast.makeText(ProfileActivity.this, "Upload Gagal: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Cloudinary Error: " + error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void updatePhotoUrlInFirestore(String url) {
        if (userRef != null) {
            userRef.update("photoUrl", url)
                    .addOnSuccessListener(aVoid -> {
                        showImageLoading(false);
                        Toast.makeText(ProfileActivity.this, "Foto profil diperbarui!", Toast.LENGTH_SHORT).show();
                        Glide.with(this).load(url).into(ivProfilePhoto);
                    })
                    .addOnFailureListener(e -> {
                        showImageLoading(false);
                        Toast.makeText(ProfileActivity.this, "Gagal menyimpan URL foto ke database", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showImageLoading(boolean isLoading) {
        pbUploadPhoto.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        fabEditPhoto.setEnabled(!isLoading);
    }

    private void loadUserProfile() {
        if (currentUser == null) {
            finish();
            return;
        }

        showLoading(true);
        userRef = db.collection("users").document(currentUser.getUid());

        userRef.get().addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                // Ambil data dari Firestore jika ada, jika tidak fallback ke Auth data
                String username = (document != null && document.contains("username")) ?
                        document.getString("username") : currentUser.getDisplayName();

                String email = currentUser.getEmail(); // Email selalu dari Auth agar akurat

                String photoUrl = (document != null && document.contains("photoUrl")) ?
                        document.getString("photoUrl") :
                        (currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);

                etUsername.setText(username);
                etEmail.setText(email);

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this).load(photoUrl).into(ivProfilePhoto);
                }
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnEditSaveProfile.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void handleEditSaveClick() {
        if (isEditMode) {
            saveUserProfile();
        } else {
            enterEditMode();
        }
    }

    private void enterEditMode() {
        isEditMode = true;
        btnEditSaveProfile.setText("Simpan Perubahan");
        etUsername.setEnabled(true);
        etUsername.requestFocus();
    }

    private void saveUserProfile() {
        String newUsername = etUsername.getText().toString().trim();
        if (TextUtils.isEmpty(newUsername)) {
            etUsername.setError("Username tidak boleh kosong");
            return;
        }

        showLoading(true);
        userRef.update("username", newUsername)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profil diperbarui", Toast.LENGTH_SHORT).show();
                    exitEditMode();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void exitEditMode() {
        isEditMode = false;
        btnEditSaveProfile.setText("Edit Profil");
        etUsername.setEnabled(false);
    }

    private void handleChangePasswordClick() {
        if (currentUser != null && currentUser.getEmail() != null) {
            // Cek apakah login pakai Google atau Email biasa
            boolean isEmailProvider = false;
            for (UserInfo info : currentUser.getProviderData()) {
                if (EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                    isEmailProvider = true;
                    break;
                }
            }

            if (isEmailProvider) {
                new AlertDialog.Builder(this)
                        .setTitle("Reset Password")
                        .setMessage("Kirim email reset password ke " + currentUser.getEmail() + "?")
                        .setPositiveButton("Ya", (d, w) -> {
                            mAuth.sendPasswordResetEmail(currentUser.getEmail())
                                    .addOnSuccessListener(v -> Toast.makeText(this, "Email reset terkirim", Toast.LENGTH_LONG).show())
                                    .addOnFailureListener(e -> Toast.makeText(this, "Gagal kirim email", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            } else {
                Toast.makeText(this, "Anda login menggunakan Google, password diatur oleh Google.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showDeleteAccountWarning() {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Akun?")
                .setMessage("Tindakan ini tidak dapat dibatalkan. Semua data favorit dan profil akan hilang.")
                .setPositiveButton("Hapus Permanen", (d, w) -> deleteUserAccount())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteUserAccount() {
        if (currentUser == null) return;
        showLoading(true);

        // Hapus data di Firestore dulu
        userRef.delete().addOnCompleteListener(task -> {
            // Lalu hapus user Auth
            currentUser.delete().addOnCompleteListener(authTask -> {
                if (authTask.isSuccessful()) {
                    Toast.makeText(this, "Akun berhasil dihapus", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    showLoading(false);
                    // Biasanya gagal karena butuh re-login (security sensitive action)
                    if (authTask.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(this, "Mohon Login ulang terlebih dahulu untuk menghapus akun.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Gagal menghapus akun: " + authTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }
}