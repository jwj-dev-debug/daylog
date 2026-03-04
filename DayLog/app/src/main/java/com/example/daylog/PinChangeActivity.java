package com.example.daylog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PinChangeActivity extends AppCompatActivity {

    private static final String PREFS = "daylog_prefs";
    private static final String KEY_PIN = "app_pin";
    private static final String KEY_LOCK_ENABLED = "lock_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_change);

        // 🔙 뒤로가기
        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 입력 칸
        EditText etOldPin        = findViewById(R.id.etOldPin);
        EditText etNewPin        = findViewById(R.id.etNewPin);
        EditText etNewPinConfirm = findViewById(R.id.etNewPinConfirm);

        Button btnSave = findViewById(R.id.btnApplyPinChange);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedPin = prefs.getString(KEY_PIN, "");

        // 🔹 기존 PIN이 없을 경우 → "현재 PIN 입력" 숨기기
        if (savedPin.isEmpty()) {
            etOldPin.setHint("현재 PIN 없음");
            etOldPin.setEnabled(false);
        }

        btnSave.setOnClickListener(v -> {

            String oldPin = etOldPin.getText().toString().trim();
            String p1     = etNewPin.getText().toString().trim();
            String p2     = etNewPinConfirm.getText().toString().trim();

            // 1️⃣ 기존 PIN 확인 (기존 PIN 있을 때만)
            if (!savedPin.isEmpty() && !oldPin.equals(savedPin)) {
                Toast.makeText(this, "현재 PIN이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2️⃣ 새 PIN 자리 수 체크
            if (p1.length() < 4) {
                Toast.makeText(this, "새 PIN은 최소 4자리입니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3️⃣ 새 PIN 일치 체크
            if (!p1.equals(p2)) {
                Toast.makeText(this, "새 PIN이 서로 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 4️⃣ 저장 + PIN 잠금 자동 활성화
            prefs.edit()
                    .putString(KEY_PIN, p1)
                    .putBoolean(KEY_LOCK_ENABLED, true)
                    .apply();

            Toast.makeText(this, "PIN이 변경되었습니다!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
