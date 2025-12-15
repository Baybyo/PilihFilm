package com.halil.ozel.moviedb.ui.auth;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.halil.ozel.moviedb.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnResetPassword;
    private TextView tvBackToLogin;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        // Init Views
        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Reset Password");
        progressDialog.setMessage("Sending email...");

        // Actions
        btnResetPassword.setOnClickListener(v -> resetPassword());

        tvBackToLogin.setOnClickListener(v -> {
            onBackPressed(); // Kembali ke activity sebelumnya (Login)
        });
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your registered email");
            return;
        }

        progressDialog.show();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Reset link sent to your email.", Toast.LENGTH_LONG).show();
                        finish(); // Tutup activity agar user kembali ke login
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Error occurred";
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}