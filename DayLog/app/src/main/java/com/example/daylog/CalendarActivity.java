package com.example.daylog;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {

    private TextView tvMonthTitle, tvPrevMonth, tvNextMonth;
    private TextView tvSettings;
    private TextView tvStats;
    private TextView[] dayViews = new TextView[42];
    private TextView tvUserGreeting;

    private int currentYear;
    private int currentMonth;

    // -------- 공통 설정 키 --------
    private static final String PREFS = "daylog_prefs";
    private static final String KEY_USER_NAME           = "user_name";
    private static final String KEY_PIN                 = "app_pin";
    private static final String KEY_LOCK_ENABLED        = "lock_enabled";
    private static final String KEY_FINGERPRINT_ENABLED = "finger_enabled";

    private static final String KEY_SELECTED_CATEGORIES = "selected_categories";
    private static final String KEY_CUSTOM_CATEGORIES   = "custom_categories";

    private static final String KEY_DAY_MARK_COLOR      = "day_mark_color";
    private static final String DEFAULT_DAY_MARK_COLOR  = "#BDE0FE";

    private static final String KEY_IS_LOGGED_IN        = "is_logged_in";

    // -------- 카테고리 상수 --------
    private static final String CAT_EXERCISE = "exercise";
    private static final String CAT_READING  = "reading";
    private static final String CAT_STUDY    = "study";
    private static final String CAT_COOKING  = "cooking";
    private static final String CAT_HOBBY    = "hobby";
    private static final String CAT_WORK     = "work";

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        db = FirebaseFirestore.getInstance();

        tvMonthTitle   = findViewById(R.id.tvMonthTitle);
        tvPrevMonth    = findViewById(R.id.tvPrevMonth);
        tvNextMonth    = findViewById(R.id.tvNextMonth);
        tvSettings     = findViewById(R.id.tvSettings);
        tvStats        = findViewById(R.id.tvStats);
        tvUserGreeting = findViewById(R.id.tvUserGreeting);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String userName = prefs.getString(KEY_USER_NAME, "");

        tvUserGreeting.setText(
                userName.isEmpty()
                        ? "오늘 하루도 기록해볼까요?"
                        : userName + "님, 오늘 하루도 기록해볼까요?"
        );

        // 날짜 42칸 초기화 (findViewById 먼저!)
        for (int i = 0; i < 42; i++) {
            int id = getResources().getIdentifier("day" + (i + 1), "id", getPackageName());
            dayViews[i] = findViewById(id);

            int index = i;
            dayViews[i].setOnClickListener(v -> {
                String dayStr = dayViews[index].getText().toString();
                if (!dayStr.isEmpty()) {
                    int day = Integer.parseInt(dayStr);
                    showRecordDialog(currentYear, currentMonth, day);
                }
            });
        }

        Calendar cal = Calendar.getInstance();
        currentYear  = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);

        // ✅ 달력 표시 (초기화 끝난 다음!)
        updateCalendar();

        // ✅ 1회 마이그레이션도 초기화 끝난 다음!
        //migrateOldRootRecordsToUserRecordsIfNeeded();

        // 이전 / 다음 달 버튼
        tvPrevMonth.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) { currentMonth = 11; currentYear--; }
            updateCalendar();
        });

        tvNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) { currentMonth = 0; currentYear++; }
            updateCalendar();
        });

        // 설정 / 통계 화면
        tvSettings.setOnClickListener(v -> showSettingsDialog());
        tvStats.setOnClickListener(v ->
                startActivity(new Intent(this, StatsActivity.class)));
    }


    // ============================================================
    // 설정 다이얼로그
    // ============================================================
    private void showSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.activity_dialog_settings);

        dialog.findViewById(R.id.tvSettingsClose)
                .setOnClickListener(v -> dialog.dismiss());

        setupCategorySection(dialog);
        setupDayColorSection(dialog);

        // PIN / 잠금 설정 화면
        dialog.findViewById(R.id.btnSetPin).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(CalendarActivity.this, LockSettingsActivity.class);
            startActivity(intent);
        });



        // 로그아웃
        dialog.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            dialog.dismiss();
            showLogoutDialog();
        });

        dialog.show();
    }

    private void showLogoutDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout);

        dialog.findViewById(R.id.btnLogoutConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });

        dialog.findViewById(R.id.btnLogoutCancel)
                .setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void performLogout() {
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception ignored) {}

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply();

        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    // ============================================================
    // 카테고리 관리
    // ============================================================
    private void setupCategorySection(Dialog dialog) {
        Set<String> selected = new HashSet<>(loadSelectedCategories());
        Set<String> custom   = new HashSet<>(loadCustomCategories());

        setupBuiltInChip(dialog, R.id.btnChipExercise, CAT_EXERCISE, selected);
        setupBuiltInChip(dialog, R.id.btnChipReading,  CAT_READING,  selected);
        setupBuiltInChip(dialog, R.id.btnChipStudy,    CAT_STUDY,    selected);
        setupBuiltInChip(dialog, R.id.btnChipCooking,  CAT_COOKING,  selected);
        setupBuiltInChip(dialog, R.id.btnChipHobby,    CAT_HOBBY,    selected);
        setupBuiltInChip(dialog, R.id.btnChipWork,     CAT_WORK,     selected);

        FlexboxLayout layout = dialog.findViewById(R.id.layoutExtraCategoryChips);
        layout.removeAllViews();

        // 이미 저장된 커스텀 칩 그리기
        for (String name : custom) {
            addExtraCategoryChip(layout, name, selected, custom);
        }

        EditText etNew = dialog.findViewById(R.id.etNewCategoryName);
        Button btnAdd  = dialog.findViewById(R.id.btnAddCategory);

        btnAdd.setOnClickListener(v -> {
            String name = etNew.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "이름을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isBuiltIn(name) || custom.contains(name)) {
                Toast.makeText(this, "이미 존재하는 카테고리", Toast.LENGTH_SHORT).show();
                return;
            }

            custom.add(name);
            selected.add(name);

            addExtraCategoryChip(layout, name, selected, custom);

            saveSelectedCategoriesToPrefs(selected);
            saveCustomCategoriesToPrefs(custom);

            etNew.setText("");
        });
    }

    private void setupBuiltInChip(Dialog dialog, int id, String tag, Set<String> selected) {
        Button btn = dialog.findViewById(id);
        boolean isSel = selected.contains(tag);
        applyChipColor(btn, isSel);

        btn.setOnClickListener(v -> {
            boolean now = !selected.contains(tag);
            if (now) selected.add(tag);
            else     selected.remove(tag);

            applyChipColor(btn, now);
            saveSelectedCategoriesToPrefs(selected);
        });
    }

    private void addExtraCategoryChip(
            FlexboxLayout parent,
            String name,
            Set<String> selected,
            Set<String> custom
    ) {
        TextView chip = new TextView(this);
        chip.setText("  " + name + "  ");
        chip.setTextSize(13);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(16), dp(10), dp(16), dp(10));
        chip.setBackgroundResource(R.drawable.bg_chip);

        boolean isSelected = selected.contains(name);
        applyChipColor(chip, isSelected);

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(dp(8), dp(8), dp(8), dp(8));
        chip.setLayoutParams(lp);

        chip.setOnClickListener(v -> {
            boolean nowSelected = !selected.contains(name);
            if (nowSelected) selected.add(name);
            else             selected.remove(name);

            applyChipColor(chip, nowSelected);
            saveSelectedCategoriesToPrefs(selected);
            saveCustomCategoriesToPrefs(custom);
        });

        chip.setOnLongClickListener(v -> {
            showDeleteCategoryDialog(name, parent, chip, selected, custom);
            return true;
        });

        parent.addView(chip);
    }

    private boolean isBuiltIn(String k) {
        return k.equals(CAT_EXERCISE) ||
                k.equals(CAT_READING)  ||
                k.equals(CAT_STUDY)    ||
                k.equals(CAT_COOKING)  ||
                k.equals(CAT_HOBBY)    ||
                k.equals(CAT_WORK);
    }

    // 칩 색상
    private void applyChipColor(View chip, boolean selected) {
        if (selected) {
            chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3B82F6")));
            if (chip instanceof TextView) ((TextView) chip).setTextColor(Color.WHITE);
            if (chip instanceof Button)   ((Button) chip).setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E7EB")));
            if (chip instanceof TextView) ((TextView) chip).setTextColor(Color.BLACK);
            if (chip instanceof Button)   ((Button) chip).setTextColor(Color.BLACK);
        }
    }

    // 카테고리 삭제 다이얼로그
    private void showDeleteCategoryDialog(
            String name,
            FlexboxLayout parent,
            TextView chipView,
            Set<String> selected,
            Set<String> custom
    ) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_delete);

        TextView tvMessage = dialog.findViewById(R.id.tvMessage);
        tvMessage.setText("카테고리 '" + name + "'을(를) 삭제할까요?");

        dialog.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            custom.remove(name);
            selected.remove(name);

            saveCustomCategoriesToPrefs(custom);
            saveSelectedCategoriesToPrefs(selected);

            parent.removeView(chipView);

            Toast.makeText(this, "카테고리가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.btnCancel)
                .setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ============================================================
    // 날짜 색상 선택
    // ============================================================
    private void setupDayColorSection(Dialog dialog) {
        View c1 = dialog.findViewById(R.id.colorOption1);
        View c2 = dialog.findViewById(R.id.colorOption2);
        View c3 = dialog.findViewById(R.id.colorOption3);
        View c4 = dialog.findViewById(R.id.colorOption4);

        View.OnClickListener listener = v -> {
            String color = DEFAULT_DAY_MARK_COLOR;

            if (v.getId() == R.id.colorOption1) color = "#BDE0FE";
            else if (v.getId() == R.id.colorOption2) color = "#FFD6A5";
            else if (v.getId() == R.id.colorOption3) color = "#C4F8C1";
            else if (v.getId() == R.id.colorOption4) color = "#FFADAD";

            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            prefs.edit().putString(KEY_DAY_MARK_COLOR, color).apply();

            updateCalendar();
            Toast.makeText(this, "색상이 변경되었습니다!", Toast.LENGTH_SHORT).show();
        };

        c1.setOnClickListener(listener);
        c2.setOnClickListener(listener);
        c3.setOnClickListener(listener);
        c4.setOnClickListener(listener);
    }

    // ============================================================
    // 달력 업데이트 (표시 + Firestore에서 마커 불러오기)
    // ============================================================
    private void updateCalendar() {
        tvMonthTitle.setText(
                String.format(Locale.KOREA, "%d년 %d월", currentYear, currentMonth + 1));

        for (int i = 0; i < 42; i++) {
            dayViews[i].setText("");
            dayViews[i].setBackground(null);
            dayViews[i].setBackgroundTintList(null);
            dayViews[i].setTypeface(null, Typeface.NORMAL);
        }

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);

        int first = cal.get(Calendar.DAY_OF_WEEK);
        int last  = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int start = first - 1;

        for (int day = 1; day <= last; day++) {
            int index = start + day - 1;
            dayViews[index].setText(String.valueOf(day));
        }

        // 🔥 Firestore에서 이 달의 기록이 있는 날짜 마커 표시
        loadMarkedDaysFromFirestore();
    }

    private void loadMarkedDaysFromFirestore() {
        for (int i = 0; i < 42; i++) {
            dayViews[i].setBackground(null);
            dayViews[i].setBackgroundTintList(null);
            dayViews[i].setTypeface(null, Typeface.NORMAL);
        }

        CollectionReference ref = userRecordsRef();
        if (ref == null) return;

        ref.whereEqualTo("year", currentYear)
                .whereEqualTo("month", currentMonth + 1)
                .get()
                .addOnSuccessListener(query -> {

                    Calendar cal = Calendar.getInstance();
                    cal.set(currentYear, currentMonth, 1);
                    int first = cal.get(Calendar.DAY_OF_WEEK);
                    int start = first - 1;
                    int last  = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

                    for (DocumentSnapshot doc : query) {

                        // 🔥 year/month/day 외에 실제 기록 필드가 하나라도 있으면 마킹
                        boolean hasAnyRecord = false;
                        Map<String, Object> data = doc.getData();
                        if (data == null) continue;

                        for (String key : data.keySet()) {
                            if (META_KEYS.contains(key)) continue;

                            Object val = data.get(key);
                            if (val instanceof String && !((String) val).trim().isEmpty()) {
                                hasAnyRecord = true;
                                break;
                            }
                        }
                        if (!hasAnyRecord) continue;

                        Long dLong = doc.getLong("day");
                        if (dLong == null) continue;
                        int day = dLong.intValue();
                        if (day < 1 || day > last) continue;

                        int index = start + day - 1;
                        markDay(index);
                    }
                });
    }


    private void markDay(int index) {
        if (index < 0 || index >= dayViews.length) return;

        TextView tv = dayViews[index];
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String c = prefs.getString(KEY_DAY_MARK_COLOR, DEFAULT_DAY_MARK_COLOR);

        tv.setBackgroundResource(R.drawable.bg_day_has_record);
        tv.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(c)));
        tv.setTypeface(null, Typeface.BOLD);
    }

    // ============================================================
    // 기록 다이얼로그 (Firestore에서 불러오기 + 저장)
    // ============================================================
    private void showRecordDialog(int year, int month, int day) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.activity_dialog_record);

        TextView tvDate = dialog.findViewById(R.id.tvSelectedDate);
        tvDate.setText(String.format("%d년 %d월 %d일", year, month + 1, day));

        LinearLayout container = dialog.findViewById(R.id.layoutActivityList);

        Set<String> selected = loadSelectedCategories();

        addDefaultCard(dialog, R.id.cardExercise, CAT_EXERCISE, selected);
        addDefaultCard(dialog, R.id.cardReading,  CAT_READING,  selected);
        addDefaultCard(dialog, R.id.cardStudy,    CAT_STUDY,    selected);
        addDefaultCard(dialog, R.id.cardCooking,  CAT_COOKING,  selected);
        addDefaultCard(dialog, R.id.cardHobby,    CAT_HOBBY,    selected);
        addDefaultCard(dialog, R.id.cardWork,     CAT_WORK,     selected);

        for (String c : selected) {
            if (!isBuiltIn(c)) addCustomRecordCard(container, c);
        }

        // 🔥 Firestore에서 해당 날짜 기록 불러오기
        loadRecordsFromFirestore(dialog, container, year, month, day);


        dialog.findViewById(R.id.tvClose).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btnSave).setOnClickListener(v -> {
            saveRecordsToFirestore(dialog, container, year, month, day); // ✅ dialog 추가
            dialog.dismiss();
        });


        dialog.show();
    }

    private void addDefaultCard(Dialog dialog, int id, String tag, Set<String> selected) {
        View v = dialog.findViewById(id);
        if (v == null) return;

        v.setTag(tag);

        if (selected.isEmpty() || selected.contains(tag))
            v.setVisibility(View.VISIBLE);
        else
            v.setVisibility(View.GONE);
    }

    private CollectionReference userRecordsRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null; //로그인 안 된 상태 방어
        return db.collection("users")
                .document(user.getUid())
                .collection("records");
    }
    private static final Set<String> META_KEYS =
            new HashSet<>(Arrays.asList("year", "month", "day"));

    private void loadRecordsFromFirestore(Dialog dialog, LinearLayout container, int year, int month, int day) {
        String docId = year + "_" + (month + 1) + "_" + day;

        CollectionReference ref = userRecordsRef();
        if (ref == null) return;

        // 기본카드 + 커스텀카드 모두 메모 초기화(잔상 방지)
        clearAllMemos(dialog, container);

        ref.document(docId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            Map<String, Object> map = doc.getData();
            if (map == null) return;

            // 🔥 문서에 존재하는 모든 카테고리 필드를 보고 UI 복원
            for (String key : map.keySet()) {
                if (META_KEYS.contains(key)) continue;

                if (isBuiltIn(key)) {
                    showBuiltInCardIfNeeded(dialog, key); // 기본카드도 과거 기록 있으면 보이게
                } else {
                    ensureCategoryCardExists(container, key); // 커스텀카드는 없으면 생성
                }
            }

            // 값 채우기
            fillMemosFromDoc(dialog, container, doc);
        });
    }


    private void showBuiltInCardIfNeeded(Dialog dialog, String cat) {
        int id = 0;
        switch (cat) {
            case CAT_EXERCISE: id = R.id.cardExercise; break;
            case CAT_READING:  id = R.id.cardReading;  break;
            case CAT_STUDY:    id = R.id.cardStudy;    break;
            case CAT_COOKING:  id = R.id.cardCooking;  break;
            case CAT_HOBBY:    id = R.id.cardHobby;    break;
            case CAT_WORK:     id = R.id.cardWork;     break;
        }
        if (id != 0) {
            View v = dialog.findViewById(id);
            if (v != null) v.setVisibility(View.VISIBLE);
        }
    }




    private void clearMemoInCard(LinearLayout card) {
        if (card == null) return;
        EditText memo = findMemo(card);
        if (memo != null) memo.setText("");
    }



    private void ensureCategoryCardExists(LinearLayout container, String category) {
        // 이미 있으면 패스
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            Object tag = child.getTag();
            if (tag != null && category.equals(tag.toString())) return;
        }

        // 기본 카테고리는 이미 XML 카드가 존재하니까 여기선 커스텀만 생성하면 됨
        if (!isBuiltIn(category)) {
            addCustomRecordCard(container, category);
        }
    }


    private void clearAllMemos(Dialog dialog, LinearLayout container) {
        // 기본 카드들(EditText 초기화)
        clearMemoInCard((LinearLayout) dialog.findViewById(R.id.cardExercise));
        clearMemoInCard((LinearLayout) dialog.findViewById(R.id.cardReading));
        clearMemoInCard((LinearLayout) dialog.findViewById(R.id.cardStudy));
        clearMemoInCard((LinearLayout) dialog.findViewById(R.id.cardCooking));
        clearMemoInCard((LinearLayout) dialog.findViewById(R.id.cardHobby));
        clearMemoInCard((LinearLayout) dialog.findViewById(R.id.cardWork));

        // 커스텀 카드들(container 안)
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof LinearLayout) {
                EditText memo = findMemo((LinearLayout) child);
                if (memo != null) memo.setText("");
            }
        }
    }



    private void fillMemosFromDoc(Dialog dialog, LinearLayout container, DocumentSnapshot doc) {
        // 기본 카드들 값 채우기
        fillOne((LinearLayout) dialog.findViewById(R.id.cardExercise), CAT_EXERCISE, doc);
        fillOne((LinearLayout) dialog.findViewById(R.id.cardReading),  CAT_READING,  doc);
        fillOne((LinearLayout) dialog.findViewById(R.id.cardStudy),    CAT_STUDY,    doc);
        fillOne((LinearLayout) dialog.findViewById(R.id.cardCooking),  CAT_COOKING,  doc);
        fillOne((LinearLayout) dialog.findViewById(R.id.cardHobby),    CAT_HOBBY,    doc);
        fillOne((LinearLayout) dialog.findViewById(R.id.cardWork),     CAT_WORK,     doc);

        // 커스텀 카드들 값 채우기
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;

            LinearLayout card = (LinearLayout) child;
            Object tagObj = card.getTag();
            if (tagObj == null) continue;
            String cat = tagObj.toString();

            EditText memo = findMemo(card);
            if (memo == null) continue;

            String value = doc.getString(cat);
            memo.setText(value != null ? value : "");
        }
    }

    private void fillOne(LinearLayout card, String cat, DocumentSnapshot doc) {
        if (card == null) return;
        EditText memo = findMemo(card);
        if (memo == null) return;

        String value = doc.getString(cat);
        memo.setText(value != null ? value : "");
    }



    // ============================================================
    // 커스텀 레코드 카드 UI
    // ============================================================

    private void addCustomRecordCard(LinearLayout container, String name) {

        LinearLayout card = new LinearLayout(this);
        card.setTag(name);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundColor(Color.parseColor("#F5F7FA"));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = new TextView(this);
        icon.setText("•");
        icon.setTextSize(18);
        icon.setPadding(0, 0, dp(6), 0);

        TextView title = new TextView(this);
        title.setText(name);
        title.setTextSize(14);
        title.setTypeface(null, Typeface.BOLD);

        top.addView(icon);
        top.addView(title);

        EditText memo = new EditText(this);
        LinearLayout.LayoutParams memoLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(100)
        );
        memoLp.setMargins(0, dp(10), 0, 0);
        memo.setLayoutParams(memoLp);
        memo.setHint(name + " 내용을 기록해주세요…");
        memo.setBackgroundColor(Color.WHITE);
        memo.setPadding(dp(10), dp(10), dp(10), dp(10));
        memo.setGravity(Gravity.TOP | Gravity.START);
        memo.setMaxLines(5);

        card.addView(top);
        card.addView(memo);
        container.addView(card);
    }
    // ============================================================
    // 지문 잠금 설정
    // ============================================================

    private void enableFingerprintLock() {
        androidx.biometric.BiometricPrompt bio =
                new androidx.biometric.BiometricPrompt(
                        this,
                        java.util.concurrent.Executors.newSingleThreadExecutor(),
                        new androidx.biometric.BiometricPrompt.AuthenticationCallback() {

                            @Override
                            public void onAuthenticationSucceeded(
                                    androidx.biometric.BiometricPrompt.AuthenticationResult result) {

                                SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                                prefs.edit()
                                        .putBoolean(KEY_FINGERPRINT_ENABLED, true)
                                        .putBoolean(KEY_LOCK_ENABLED, true)
                                        .apply();

                                runOnUiThread(() ->
                                        Toast.makeText(
                                                CalendarActivity.this,
                                                "지문 잠금이 활성화되었습니다",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );
                            }

                            @Override
                            public void onAuthenticationError(int code, CharSequence msg) {
                                runOnUiThread(() ->
                                        Toast.makeText(CalendarActivity.this,
                                                "오류: " + msg,
                                                Toast.LENGTH_SHORT).show()
                                );
                            }
                        });

        androidx.biometric.BiometricPrompt.PromptInfo info =
                new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                        .setTitle("지문 인증")
                        .setSubtitle("지문으로 인증하세요")
                        .setNegativeButtonText("취소")
                        .build();

        bio.authenticate(info);
    }
    // ============================================================
    // SharedPreferences 카테고리 로드/저장
    // ============================================================

    private Set<String> loadSelectedCategories() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString(KEY_SELECTED_CATEGORIES, "");
        Set<String> set = new HashSet<>();
        if (saved.isEmpty()) return set;

        for (String s : saved.split(",")) {
            if (!s.trim().isEmpty()) set.add(s.trim());
        }
        return set;
    }
    private Set<String> loadCustomCategories() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString(KEY_CUSTOM_CATEGORIES, "");
        Set<String> set = new HashSet<>();
        if (saved.isEmpty()) return set;

        for (String s : saved.split(",")) {
            if (!s.trim().isEmpty()) set.add(s.trim());
        }
        return set;
    }

    private void saveSelectedCategoriesToPrefs(Set<String> set) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString(KEY_SELECTED_CATEGORIES, String.join(",", set)).apply();
    }

    private void saveRecordsToFirestore(
            Dialog dialog,
            LinearLayout container,
            int year,
            int month,
            int day
    ) {
        CollectionReference ref = userRecordsRef();
        if (ref == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String docId = year + "_" + (month + 1) + "_" + day;

        // 1) 이번 화면의 "입력값"을 data에 모으기
        Map<String, Object> data = new HashMap<>();
        data.put("year", year);
        data.put("month", month + 1);
        data.put("day", day);

        boolean hasAnyRecord = false;

        // ✅ 기본 카드들(다이얼로그 안)
        hasAnyRecord |= putIfNotEmpty(data, CAT_EXERCISE, (LinearLayout) dialog.findViewById(R.id.cardExercise));
        hasAnyRecord |= putIfNotEmpty(data, CAT_READING,  (LinearLayout) dialog.findViewById(R.id.cardReading));
        hasAnyRecord |= putIfNotEmpty(data, CAT_STUDY,    (LinearLayout) dialog.findViewById(R.id.cardStudy));
        hasAnyRecord |= putIfNotEmpty(data, CAT_COOKING,  (LinearLayout) dialog.findViewById(R.id.cardCooking));
        hasAnyRecord |= putIfNotEmpty(data, CAT_HOBBY,    (LinearLayout) dialog.findViewById(R.id.cardHobby));
        hasAnyRecord |= putIfNotEmpty(data, CAT_WORK,     (LinearLayout) dialog.findViewById(R.id.cardWork));

        // ✅ 커스텀 카드들(container 안)
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;

            LinearLayout card = (LinearLayout) child;
            Object tagObj = card.getTag();
            if (tagObj == null) continue;

            String cat = tagObj.toString();
            EditText memo = findMemo(card);
            if (memo == null) continue;

            String text = memo.getText().toString().trim();
            if (!text.isEmpty()) {
                data.put(cat, text);
                hasAnyRecord = true;
            }
        }

        // 2) ✅ 전부 비었으면 문서 삭제(마커 제거 목적)
        if (!hasAnyRecord) {
            ref.document(docId)
                    .delete()
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "기록이 삭제되었습니다!", Toast.LENGTH_SHORT).show();
                        updateCalendar();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "삭제 오류 발생", Toast.LENGTH_SHORT).show());
            return;
        }

        // 3) ✅ 하나라도 있으면 "전체 덮어쓰기"로 저장해야 지운 값이 반영됨
        ref.document(docId)
                .set(data) // ❗ merge() 쓰지 않음
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "기록이 저장되었습니다!", Toast.LENGTH_SHORT).show();
                    updateCalendar();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 오류 발생", Toast.LENGTH_SHORT).show());
    }




    private void saveCustomCategoriesToPrefs(Set<String> set) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString(KEY_CUSTOM_CATEGORIES, String.join(",", set)).apply();
    }

    // dp 변환
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);

    }
    private EditText findMemo(LinearLayout card) {
        for (int i = 0; i < card.getChildCount(); i++) {
            View v = card.getChildAt(i);
            if (v instanceof EditText) return (EditText) v;

            if (v instanceof LinearLayout) {
                EditText e = findMemo((LinearLayout) v);
                if (e != null) return e;
            }
        }
        return null;
    }

    private boolean putIfNotEmpty(Map<String, Object> data, String cat, LinearLayout card) {
        if (card == null) return false;
        EditText memo = findMemo(card);
        if (memo == null) return false;

        String text = memo.getText().toString().trim();
        if (!text.isEmpty()) {
            data.put(cat, text);
            return true;
        }
        return false;
    }

    private static final String KEY_MIGRATED_TO_USER_RECORDS = "migrated_to_user_records";



    private void migrateOldRootRecordsToUserRecordsIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_MIGRATED_TO_USER_RECORDS, false)) return;

        CollectionReference newRef = userRecordsRef();
        if (newRef == null) return; // 로그인 안됨

        // ✅ db가 null이면 안 되니까 방어(혹시라도 순서 꼬일 때)
        if (db == null) db = FirebaseFirestore.getInstance();

        db.collection("records")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        prefs.edit().putBoolean(KEY_MIGRATED_TO_USER_RECORDS, true).apply();
                        Toast.makeText(this, "이전 기록이 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int totalDocs = query.size();
                    int copiedTotal = 0;
                    int skipped = 0;

                    WriteBatch batch = db.batch();
                    int batchCount = 0;

                    // 마지막 커밋 완료 후 prefs 처리 위해 pending 커밋 카운트 관리
                    final int[] pendingCommits = {0};
                    final boolean[] anyFailure = {false};

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data == null) { skipped++; continue; }

                        // 1) 문서 필드에서 우선 시도
                        Integer y = toIntOrNull(doc.get("year"));
                        Integer m = toIntOrNull(doc.get("month"));
                        Integer d = toIntOrNull(doc.get("day"));

                        // 2) 없으면 문서ID에서 파싱 시도
                        if (y == null || m == null || d == null) {
                            int[] ymd = parseYmdFromDocId(doc.getId()); // [y,m,d] or null
                            if (ymd != null) {
                                y = ymd[0];
                                m = ymd[1];
                                d = ymd[2];
                            }
                        }

                        if (y == null || m == null || d == null) {
                            skipped++;
                            continue;
                        }

                        // ✅ 새 문서ID 통일
                        String newDocId = y + "_" + m + "_" + d;

                        // ✅ 새 문서로 복사할 data에 year/month/day가 없으면 넣어주기
                        Map<String, Object> newData = new HashMap<>(data);
                        newData.put("year", y);
                        newData.put("month", m);
                        newData.put("day", d);

                        batch.set(newRef.document(newDocId), newData);
                        copiedTotal++;
                        batchCount++;

                        // ✅ 500개마다 커밋
                        if (batchCount == 450) { // 500보다 여유 두면 안전(필요없지만 권장)
                            pendingCommits[0]++;
                            int commitCopiedSoFar = copiedTotal;
                            int commitSkippedSoFar = skipped;

                            batch.commit()
                                    .addOnSuccessListener(v -> {
                                        pendingCommits[0]--;
                                        // 모든 커밋 끝났는지 체크
                                        if (pendingCommits[0] == 0 && !anyFailure[0]) {
                                            prefs.edit().putBoolean(KEY_MIGRATED_TO_USER_RECORDS, true).apply();
                                            Toast.makeText(
                                                    this,
                                                    "이전 완료! 복사 " + commitCopiedSoFar + "개 / 스킵 " + commitSkippedSoFar + "개",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                            updateCalendar();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        pendingCommits[0]--;
                                        anyFailure[0] = true;
                                        Toast.makeText(this, "마이그레이션 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });

                            // 새 배치 시작
                            batch = db.batch();
                            batchCount = 0;
                        }
                    }

                    // ✅ 남은 것 커밋
                    if (batchCount > 0) {
                        pendingCommits[0]++;

                        int finalCopied = copiedTotal;
                        int finalSkipped = skipped;

                        batch.commit()
                                .addOnSuccessListener(v -> {
                                    pendingCommits[0]--;
                                    if (pendingCommits[0] == 0 && !anyFailure[0]) {
                                        prefs.edit().putBoolean(KEY_MIGRATED_TO_USER_RECORDS, true).apply();
                                        Toast.makeText(
                                                this,
                                                "이전 완료! 복사 " + finalCopied + "개 / 스킵 " + finalSkipped + "개 (총 " + totalDocs + "개 중)",
                                                Toast.LENGTH_LONG
                                        ).show();
                                        updateCalendar();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    pendingCommits[0]--;
                                    anyFailure[0] = true;
                                    Toast.makeText(this, "마이그레이션 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        // 배치가 비었는데도 copiedTotal이 0일 수 있음(전부 스킵)
                        if (!anyFailure[0]) {
                            prefs.edit().putBoolean(KEY_MIGRATED_TO_USER_RECORDS, true).apply();
                            Toast.makeText(
                                    this,
                                    "이전 완료(복사 0 / 스킵 " + skipped + "개). 문서ID 형식 확인 필요",
                                    Toast.LENGTH_LONG
                            ).show();
                            updateCalendar();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "이전 데이터 조회 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // -------------------- 헬퍼 1: Object -> Integer --------------------
    private Integer toIntOrNull(Object o) {
        if (o == null) return null;
        if (o instanceof Long) return ((Long) o).intValue();
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof Double) return ((Double) o).intValue();
        if (o instanceof String) {
            try { return Integer.parseInt(((String) o).trim()); } catch (Exception ignored) {}
        }
        return null;
    }

    // -------------------- 헬퍼 2: 문서ID에서 날짜 파싱 --------------------
    private int[] parseYmdFromDocId(String id) {
        if (id == null) return null;
        String s = id.trim();

        // 허용 예:
        // "2026_1_29"
        // "2026-01-29"
        // "2026.1.29"
        // "2026/1/29"
        // "2026 1 29"
        String[] parts = s.split("[^0-9]+");
        if (parts.length < 3) return null;

        try {
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int d = Integer.parseInt(parts[2]);

            // 간단 검증
            if (y < 2000 || y > 2100) return null;
            if (m < 1 || m > 12) return null;
            if (d < 1 || d > 31) return null;

            return new int[]{y, m, d};
        } catch (Exception e) {
            return null;
        }
    }


}
