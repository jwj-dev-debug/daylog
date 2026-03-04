package com.example.daylog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PinLockActivity extends AppCompatActivity {

    private static final String PREFS = "daylog_prefs";
    private static final String KEY_PIN = "app_pin";
    private static final String KEY_FINGERPRINT_ENABLED = "finger_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_lock);

        EditText etPin = findViewById(R.id.etPinInput);
        Button btnEnter = findViewById(R.id.btnEnter);
        Button btnUseFingerprint = findViewById(R.id.btnUseFingerprint);
        TextView btnForgotPin = findViewById(R.id.btnForgotPin);   // ← 추가됨

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedPin = prefs.getString(KEY_PIN, "");
        boolean fpEnabled = prefs.getBoolean(KEY_FINGERPRINT_ENABLED, false);

        // 지문 버튼 숨기기/보이기
        if (!fpEnabled) {
            btnUseFingerprint.setVisibility(Button.GONE);
        }

        // PIN 확인
        btnEnter.setOnClickListener(v -> {
            String input = etPin.getText().toString().trim();

            if (input.equals(savedPin)) {
                goToMain();
            } else {
                Toast.makeText(this, "PIN이 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            }
        });

        // 지문으로 해제
        btnUseFingerprint.setOnClickListener(v -> {
            Intent intent = new Intent(this, LockActivity.class);
            intent.putExtra("force_fingerprint", true);
            startActivity(intent);
            finish();
        });

        // 🔥 PIN을 잊으셨나요 → PIN 재설정 화면 이동
        btnForgotPin.setOnClickListener(v -> {
            Intent intent = new Intent(this, PinResetActivity.class);
            startActivity(intent);
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, CalendarActivity.class));
        finish();
    }
}
