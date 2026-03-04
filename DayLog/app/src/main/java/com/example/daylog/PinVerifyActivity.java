package com.example.daylog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PinVerifyActivity extends AppCompatActivity {

    private static final String PREFS = "daylog_prefs";
    private static final String KEY_PIN = "app_pin";

    public static final String TARGET_ACTION = "target_action";
    public static final String ACTION_CHANGE_PIN = "change_pin";
    public static final String ACTION_CHANGE_LOCK = "change_lock";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_verify);

        EditText etPin = findViewById(R.id.etVerifyPin);
        Button btnCheck = findViewById(R.id.btnVerifyPin);
        Button btnCancel = findViewById(R.id.btnCancel);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedPin = prefs.getString(KEY_PIN, "");

        String action = getIntent().getStringExtra(TARGET_ACTION);

        btnCheck.setOnClickListener(v -> {
            String input = etPin.getText().toString().trim();

            if (input.equals(savedPin)) {

                // PIN 일치 → 다음 화면 이동
                if (ACTION_CHANGE_PIN.equals(action)) {
                    startActivity(new Intent(this, PinChangeActivity.class));
                }

                else if (ACTION_CHANGE_LOCK.equals(action)) {
                    // 설정 화면으로 이동
                    startActivity(new Intent(this, LockSettingsActivity.class));
                }

                finish();
            } else {
                Toast.makeText(this, "PIN이 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> finish());
    }
}
