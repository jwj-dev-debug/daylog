package com.example.daylog;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StatsActivity extends AppCompatActivity {

    private static final String PREFS = "daylog_prefs";
    private static final String KEY_SELECTED_CATEGORIES = "selected_categories";
    private static final String KEY_CUSTOM_CATEGORIES   = "custom_categories";

    private static final String CAT_EXERCISE = "exercise";
    private static final String CAT_READING  = "reading";
    private static final String CAT_STUDY    = "study";
    private static final String CAT_COOKING  = "cooking";
    private static final String CAT_HOBBY    = "hobby";
    private static final String CAT_WORK     = "work";

    private TextView tvStatsMonthTitle, tvTotalRecordDays, tvTotalRecords, tvTopCategory;
    private LinearLayout layoutBarChart;

    private int currentYear, currentMonth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        db = FirebaseFirestore.getInstance();

        tvStatsMonthTitle = findViewById(R.id.tvStatsMonthTitle);
        tvTotalRecordDays = findViewById(R.id.tvTotalRecordDays);
        tvTotalRecords    = findViewById(R.id.tvTotalRecords);
        tvTopCategory     = findViewById(R.id.tvTopCategory);
        layoutBarChart    = findViewById(R.id.layoutBarChart);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        findViewById(R.id.tvPrevMonth).setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) { currentMonth = 11; currentYear--; }
            loadStats();
        });

        findViewById(R.id.tvNextMonth).setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) { currentMonth = 0; currentYear++; }
            loadStats();
        });

        Calendar c = Calendar.getInstance();
        currentYear  = c.get(Calendar.YEAR);
        currentMonth = c.get(Calendar.MONTH);

        loadStats();
    }

    private void loadStats() {

        db.collection("records")
                .get()
                .addOnSuccessListener(query -> {

                    Set<String> countedDays = new HashSet<>();
                    Map<String, Integer> countMap = new HashMap<>();

                    Set<String> base   = loadSelectedCategories();
                    Set<String> custom = loadCustomCategories();

                    for (String c : base)   countMap.put(c, 0);
                    for (String c : custom) countMap.put(c, 0);

                    int totalRecords = 0;

                    for (var doc : query) {

                        Integer y = doc.getLong("year")  == null ? null : doc.getLong("year").intValue();
                        Integer m = doc.getLong("month") == null ? null : doc.getLong("month").intValue();
                        Integer d = doc.getLong("day")   == null ? null : doc.getLong("day").intValue();

                        if (y == null || m == null || d == null) continue;

                        if (y != currentYear) continue;
                        if (m < 1 || m > 12)  continue;
                        if (m != currentMonth + 1) continue;

                        String dateKey = y + "_" + m + "_" + d;
                        countedDays.add(dateKey);

                        for (String key : countMap.keySet()) {
                            String value = doc.getString(key);
                            if (value != null && !value.isEmpty()) {
                                countMap.put(key, countMap.get(key) + 1);
                                totalRecords++;
                            }
                        }
                    }

                    updateUI(countMap, countedDays.size(), totalRecords);

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "통계 불러오기 실패", Toast.LENGTH_SHORT).show());
    }

    private void updateUI(Map<String, Integer> counts, int totalDays, int totalRec) {

        tvStatsMonthTitle.setText(
                String.format(Locale.KOREA, "%d년 %d월 통계",
                        currentYear, currentMonth + 1)
        );

        tvTotalRecordDays.setText(totalDays + "일");
        tvTotalRecords.setText(totalRec + "개");

        int max = 0;
        String top = "기록 없음";

        for (var e : counts.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                top = getLabel(e.getKey());
            }
        }

        if (max == 0) top = "기록 없음";
        tvTopCategory.setText(top);

        showBar(counts);
    }

    private void showBar(Map<String, Integer> counts) {

        layoutBarChart.removeAllViews();

        int max = 0;
        for (int v : counts.values()) if (v > max) max = v;

        int maxHeight = 120;

        for (var e : counts.entrySet()) {

            String key = e.getKey();
            int v      = e.getValue();

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1));

            int barH = (max == 0) ? 8 :
                    Math.max(8, Math.round((v * maxHeight) / (float) max));

            View bar = new View(this);
            LinearLayout.LayoutParams bp =
                    new LinearLayout.LayoutParams(dp(22), dp(barH));
            bar.setLayoutParams(bp);
            bar.setBackgroundColor(Color.parseColor("#A5C8FF"));

            TextView lb = new TextView(this);
            lb.setText(getLabel(key));
            lb.setTextSize(11);
            lb.setGravity(Gravity.CENTER);

            col.addView(bar);
            col.addView(lb);

            layoutBarChart.addView(col);
        }
    }

    private String getLabel(String key) {
        switch (key) {
            case CAT_EXERCISE: return "운동";
            case CAT_READING:  return "독서";
            case CAT_STUDY:    return "공부";
            case CAT_COOKING:  return "요리";
            case CAT_HOBBY:    return "취미";
            case CAT_WORK:     return "업무";
            default:           return key;  // 커스텀 카테고리
        }
    }

    private Set<String> loadSelectedCategories() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String s = prefs.getString(KEY_SELECTED_CATEGORIES, "");
        Set<String> out = new HashSet<>();
        if (s.isEmpty()) return out;
        for (String x : s.split(",")) if (!x.isEmpty()) out.add(x);
        return out;
    }

    private Set<String> loadCustomCategories() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String s = prefs.getString(KEY_CUSTOM_CATEGORIES, "");
        Set<String> out = new HashSet<>();
        if (s.isEmpty()) return out;
        for (String x : s.split(",")) if (!x.isEmpty()) out.add(x);
        return out;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
