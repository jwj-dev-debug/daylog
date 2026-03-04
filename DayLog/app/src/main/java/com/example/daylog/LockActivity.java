package com.example.daylog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;

import java.util.concurrent.Executors;

public class LockActivity extends AppCompatActivity {

    private static final String PREFS = "daylog_prefs";
    private static final String KEY_PIN = "app_pin";
    private static final String KEY_LOCK_ENABLED = "lock_enabled";
    private static final String KEY_FINGERPRINT_ENABLED = "finger_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        boolean pinEnabled = prefs.getBoolean(KEY_LOCK_ENABLED, false);
        boolean fpEnabled  = prefs.getBoolean(KEY_FINGERPRINT_ENABLED, false);

        // 🔹 지문 인증이 켜져 있으면 지문 먼저 실행
        if (fpEnabled) {
            showFingerprintDialog();
            return;
        }

        // 🔹 PIN만 켜져 있는 경우
        if (pinEnabled) {
            startActivity(new Intent(this, PinLockActivity.class));
            finish();
            return;
        }

        // 🔹 잠금 기능 꺼졌으면 바로 캘린더로
        startActivity(new Intent(this, CalendarActivity.class));
        finish();
    }

    private void showFingerprintDialog() {

        BiometricPrompt prompt = new BiometricPrompt(
                this,
                Executors.newSingleThreadExecutor(),
                new BiometricPrompt.AuthenticationCallback() {

                    // 🔥 지문 인증 성공
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        runOnUiThread(() -> {
                            startActivity(new Intent(LockActivity.this, CalendarActivity.class));
                            finish();
                        });
                    }

                    // 🔥 오류 발생 (사용자가 "PIN 사용" 버튼 클릭 포함)
                    @Override
                    public void onAuthenticationError(
                            int errorCode, @NonNull CharSequence errString) {

                        runOnUiThread(() -> {

                            // "PIN 사용" 버튼 눌렀을 때 발생하는 코드
                            if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                startActivity(new Intent(LockActivity.this, PinLockActivity.class));
                                finish();
                                return;
                            }

                            // 그 외 오류 → PIN으로 fallback
                            startActivity(new Intent(LockActivity.this, PinLockActivity.class));
                            finish();
                        });
                    }

                    // 🔥 지문 실패 (재시도 가능한 경우)
                    @Override
                    public void onAuthenticationFailed() {
                        // 실패해도 프롬프트는 그대로 유지됨
                    }
                });

        BiometricPrompt.PromptInfo info =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("DayLog 잠금 해제")
                        .setSubtitle("지문으로 잠금을 해제하세요")
                        // ❗ 반드시 있어야 “PIN 사용하기” 버튼이 생김
                        .setNegativeButtonText("PIN 사용")
                        .build();

        prompt.authenticate(info);
    }
}
