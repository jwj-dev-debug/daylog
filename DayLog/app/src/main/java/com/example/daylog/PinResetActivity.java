package com.example.daylog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class PinResetActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnReset;

    private static final String PREFS = "daylog_prefs";
    private static final String KEY_PIN = "app_pin";
    private static final String KEY_LOCK_ENABLED = "lock_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_reset);

        etEmail = findViewById(R.id.etResetEmail);
        etPassword = findViewById(R.id.etResetPassword);
        btnReset = findViewById(R.id.btnResetPin);

        btnReset.setOnClickListener(v -> resetPin());
    }

    private void resetPin() {

        String email = etEmail.getText().toString().trim();
        String pw = etPassword.getText().toString().trim();

        if (email.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, pw)
                .addOnSuccessListener(auth -> {

                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                    prefs.edit()
                            .remove(KEY_PIN)
                            .putBoolean(KEY_LOCK_ENABLED, false)
                            .apply();

                    Toast.makeText(this, "PIN이 초기화되었습니다!", Toast.LENGTH_SHORT).show();

                    // 홈 화면으로 이동
                    Intent i = new Intent(this, CalendarActivity.class);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
