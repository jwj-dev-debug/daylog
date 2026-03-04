package com.example.daylog;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LockSettingsActivity extends AppCompatActivity {

    private Switch switchPinLock, switchFingerprintLock;
    private Button btnChangePin;

    private static final String PREFS = "daylog_prefs";
    private static final String KEY_PIN = "app_pin";
    private static final String KEY_LOCK_ENABLED = "lock_enabled";
    private static final String KEY_FINGERPRINT_ENABLED = "finger_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_setting);

        switchPinLock = findViewById(R.id.switchPinLock);
        switchFingerprintLock = findViewById(R.id.switchFingerprintLock);
        btnChangePin = findViewById(R.id.btnChangePin);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // ----------------------------------------------------
        // ① 잠금 기능이 실제로 켜져 있는지 확인 후 스위치 표시
        // ----------------------------------------------------
        boolean lockEnabled = prefs.getBoolean(KEY_LOCK_ENABLED, false);
        switchPinLock.setChecked(lockEnabled);

        switchFingerprintLock.setChecked(
                prefs.getBoolean(KEY_FINGERPRINT_ENABLED, false)
        );

        // ----------------------------------------------------
        // ② PIN 잠금 스위치 동작
        // ----------------------------------------------------
        switchPinLock.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                String savedPin = prefs.getString(KEY_PIN, "");

                if (savedPin.isEmpty()) {
                    // PIN 없음 → 새로 설정 필요
                    showPinSetupDialog();
                } else {
                    // PIN 존재 → 잠금만 활성화
                    prefs.edit().putBoolean(KEY_LOCK_ENABLED, true).apply();
                }

            } else {
                // 잠금 OFF
                prefs.edit().putBoolean(KEY_LOCK_ENABLED, false).apply();
            }
        });

        // ----------------------------------------------------
        // ③ 지문 스위치
        // ----------------------------------------------------
        switchFingerprintLock.setOnCheckedChangeListener((buttonView, isChecked) -> {

            String savedPin = prefs.getString(KEY_PIN, "");

            if (isChecked && savedPin.isEmpty()) {
                Toast.makeText(this, "먼저 PIN을 설정해야 합니다.", Toast.LENGTH_SHORT).show();
                switchFingerprintLock.setChecked(false);
                return;
            }

            prefs.edit().putBoolean(KEY_FINGERPRINT_ENABLED, isChecked).apply();
        });

        // ----------------------------------------------------
        // ④ PIN 변경 버튼
        // ----------------------------------------------------
        btnChangePin.setOnClickListener(v -> {
            startActivity(new android.content.Intent(
                    this,
                    PinChangeActivity.class
            ));
        });
    }

    // ============================================================
    // PIN 최초 설정 Dialog
    // ============================================================
    private void showPinSetupDialog() {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_set_pin);

        EditText etPin = dialog.findViewById(R.id.etPin);
        Button btnSave = dialog.findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {

            String pin = etPin.getText().toString().trim();

            if (pin.length() < 4) {
                Toast.makeText(this, "PIN은 4자리 이상 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // PIN 저장 + 잠금 활성화
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_PIN, pin)
                    .putBoolean(KEY_LOCK_ENABLED, true)
                    .apply();

            Toast.makeText(this, "PIN이 설정되었습니다.", Toast.LENGTH_SHORT).show();

            switchPinLock.setChecked(true);
            dialog.dismiss();
        });

        dialog.show();
    }
}
