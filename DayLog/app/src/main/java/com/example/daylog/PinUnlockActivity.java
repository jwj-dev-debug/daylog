package com.example.daylog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PinUnlockActivity extends AppCompatActivity {

    private static final String PREFS = "daylog_prefs";
    private static final String KEY_PIN = "app_pin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_unlock);

        EditText etPin = findViewById(R.id.etPinInput);
        Button btnUnlock = findViewById(R.id.btnUnlock);

        btnUnlock.setOnClickListener(v -> {
            String input = etPin.getText().toString().trim();

            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            String savedPin = prefs.getString(KEY_PIN, "");

            if (input.equals(savedPin)) {
                startActivity(new Intent(this, CalendarActivity.class));
                finish();
            } else {
                Toast.makeText(this, "PIN이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
