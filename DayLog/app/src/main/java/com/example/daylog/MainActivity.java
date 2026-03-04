package com.example.daylog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private EditText etUserName, etEmail, etPassword;
    private Button btnExercise, btnReading, btnStudy, btnCooking, btnHobby, btnWork, btnStart;

    private static final String PREFS_NAME = "daylog_prefs";
    private static final String KEY_SELECTED_CATEGORIES = "selected_categories";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_NAME = "user_name";

    private static final String CAT_EXERCISE = "exercise";
    private static final String CAT_READING = "reading";
    private static final String CAT_STUDY = "study";
    private static final String CAT_COOKING = "cooking";
    private static final String CAT_HOBBY = "hobby";
    private static final String CAT_WORK = "work";

    private Set<String> selectedCategories = new HashSet<>();

    // 🔥 Firebase Auth
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔥 Firebase Auth 객체 생성
        mAuth = FirebaseAuth.getInstance();

        // 🔥 자동 로그인(온보딩 완료 + Firebase 로그인 유지)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (isLoggedIn && currentUser != null) {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // 온보딩 화면 표시
        setContentView(R.layout.activity_main);

        etUserName = findViewById(R.id.etUserName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnExercise = findViewById(R.id.btnCategoryExercise);
        btnReading = findViewById(R.id.btnCategoryReading);
        btnStudy = findViewById(R.id.btnCategoryStudy);
        btnCooking = findViewById(R.id.btnCategoryCooking);
        btnHobby = findViewById(R.id.btnCategoryHobby);
        btnWork = findViewById(R.id.btnCategoryWork);
        btnStart = findViewById(R.id.btnStart);

        loadSelectedCategoriesFromPrefs();

        setupCategoryToggle(btnExercise, CAT_EXERCISE);
        setupCategoryToggle(btnReading, CAT_READING);
        setupCategoryToggle(btnStudy, CAT_STUDY);
        setupCategoryToggle(btnCooking, CAT_COOKING);
        setupCategoryToggle(btnHobby, CAT_HOBBY);
        setupCategoryToggle(btnWork, CAT_WORK);

        restoreCategoryButtonStyles();

        // 🔥 시작하기 버튼 → 회원가입/로그인
        btnStart.setOnClickListener(v -> {
            String userName = etUserName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (userName.isEmpty()) {
                Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidEmail(email)) {
                Toast.makeText(this, "이메일 형식을 다시 확인해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "비밀번호는 6자 이상 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            saveSelectedCategories();

            registerOrLogin(email, password, userName);
        });
    }

    // ------------------------
    // 🔥 Firebase 회원가입 또는 로그인
    // ------------------------
    private void registerOrLogin(String email, String password, String userName) {

        // 우선 회원가입 시도
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 회원가입 성공 → 바로 로그인 상태
                        onLoginSuccess(userName);
                    } else {
                        // 이미 있는 계정일 가능성 → 로그인 시도
                        mAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(loginTask -> {
                                    if (loginTask.isSuccessful()) {
                                        onLoginSuccess(userName);
                                    } else {
                                        Toast.makeText(
                                                MainActivity.this,
                                                "로그인/회원가입 실패: " + loginTask.getException().getMessage(),
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                });
                    }
                });
    }

    // ------------------------
    // 🔥 로그인 성공 처리
    // ------------------------
    private void onLoginSuccess(String userName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_NAME, userName)
                .apply();

        Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
        startActivity(intent);
        finish();
    }

    // ------------------------
    // 도구 메소드들
    // ------------------------
    private boolean isValidEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 && at < email.length() - 1;
    }

    private void setupCategoryToggle(Button button, String categoryKey) {
        setCategoryButtonStyle(button, selectedCategories.contains(categoryKey));
        button.setOnClickListener(v -> {
            if (selectedCategories.contains(categoryKey))
                selectedCategories.remove(categoryKey);
            else
                selectedCategories.add(categoryKey);
            setCategoryButtonStyle(button, selectedCategories.contains(categoryKey));
        });
    }

    private void setCategoryButtonStyle(Button button, boolean selected) {
        if (selected) {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3B82F6")));
            button.setTextColor(Color.WHITE);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E7EB")));
            button.setTextColor(Color.BLACK);
        }
    }

    private void saveSelectedCategories() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String c : selectedCategories) {
            if (i > 0) sb.append(",");
            sb.append(c);
            i++;
        }
        prefs.edit().putString(KEY_SELECTED_CATEGORIES, sb.toString()).apply();
    }

    private void loadSelectedCategoriesFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_SELECTED_CATEGORIES, "");
        selectedCategories.clear();
        if (saved.isEmpty()) return;

        for (String p : saved.split(",")) {
            if (!p.trim().isEmpty()) selectedCategories.add(p.trim());
        }
    }

    private void restoreCategoryButtonStyles() {
        setCategoryButtonStyle(btnExercise, selectedCategories.contains(CAT_EXERCISE));
        setCategoryButtonStyle(btnReading, selectedCategories.contains(CAT_READING));
        setCategoryButtonStyle(btnStudy, selectedCategories.contains(CAT_STUDY));
        setCategoryButtonStyle(btnCooking, selectedCategories.contains(CAT_COOKING));
        setCategoryButtonStyle(btnHobby, selectedCategories.contains(CAT_HOBBY));
        setCategoryButtonStyle(btnWork, selectedCategories.contains(CAT_WORK));
    }
}
