package hung.edu.mealmindai.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.MealPlan;
import hung.edu.mealmindai.repositories.MealPlanRepository;

public class TodayMealPlanActivity extends AppCompatActivity {
    private final MealPlanRepository repository = new MealPlanRepository();
    private final String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

    private LinearLayout layoutPlans;
    private View layoutState;
    private TextView textState, textDate, textTodayPlanProgress, textTodayPlanSummary;
    private ProgressBar progressBar, progressTodayCompletion;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today_meal_plan);

        initViews();
        loadPlans();
    }

    private void initViews() {
        ImageButton buttonBack = findViewById(R.id.buttonTodayPlanBack);
        rootView = findViewById(R.id.todayPlanRoot);
        layoutPlans = findViewById(R.id.layoutTodayPlans);
        layoutState = findViewById(R.id.layoutTodayPlanState);
        textState = findViewById(R.id.textTodayPlanState);
        textDate = findViewById(R.id.textTodayPlanDate);
        textTodayPlanProgress = findViewById(R.id.textTodayPlanProgress);
        textTodayPlanSummary = findViewById(R.id.textTodayPlanSummary);
        progressTodayCompletion = findViewById(R.id.progressTodayCompletion);
        progressBar = findViewById(R.id.progressTodayPlan);
        textDate.setText("Ngày " + today + " • đánh dấu món đã hoàn thành");
        buttonBack.setOnClickListener(v -> finish());
    }

    private void loadPlans() {
        setLoading(true);
        repository.loadPlansForDate(today, new MealPlanRepository.MealPlansCallback() {
            @Override
            public void onSuccess(List<MealPlan> mealPlans) {
                setLoading(false);
                renderPlans(mealPlans);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                showState("Không thể tải kế hoạch hôm nay. Vui lòng thử lại.");
                showMessage("Lỗi kế hoạch: " + e.getMessage());
            }

            @Override
            public void onLoginRequired() {
                setLoading(false);
                showState("Vui lòng đăng nhập để xem kế hoạch hôm nay.");
            }
        });
    }

    private void renderPlans(List<MealPlan> plans) {
        layoutPlans.removeAllViews();
        if (plans == null || plans.isEmpty()) {
            showState("Bạn chưa thêm món vào kế hoạch hôm nay.");
            return;
        }

        layoutState.setVisibility(View.GONE);
        layoutPlans.setVisibility(View.VISIBLE);
        updateProgressSummary(plans);
        for (MealPlan plan : plans) {
            layoutPlans.addView(createPlanCard(plan, plans));
        }
    }

    private View createPlanCard(MealPlan plan, List<MealPlan> allPlans) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.card_stroke));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(12));

        CheckBox checkBox = new CheckBox(this);
        String recipeName = plan.getRecipeName() == null || plan.getRecipeName().trim().isEmpty()
                ? "Món ăn" : plan.getRecipeName();
        checkBox.setText(recipeName);
        checkBox.setChecked(plan.getCompleted() != null && plan.getCompleted());
        checkBox.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        checkBox.setTextSize(17f);
        checkBox.setTypeface(checkBox.getTypeface(), android.graphics.Typeface.BOLD);
        checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.primary_green)));
        checkBox.setPadding(0, 0, 0, 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        checkBox.setLayoutParams(params);

        TextView meta = new TextView(this);
        meta.setText(formatMealType(plan.getMealType()) + " • "
                + (checkBox.isChecked() ? "Đã hoàn thành" : "Chưa hoàn thành"));
        meta.setTextColor(ContextCompat.getColor(this,
                checkBox.isChecked() ? R.color.primary_green : R.color.text_secondary));
        meta.setTextSize(13f);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        metaParams.setMargins(dp(42), dp(4), 0, 0);
        meta.setLayoutParams(metaParams);

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                repository.updateCompleted(plan.getMealPlanId(), isChecked,
                        new MealPlanRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                plan.setCompleted(isChecked);
                                meta.setText(formatMealType(plan.getMealType()) + " • "
                                        + (isChecked ? "Đã hoàn thành" : "Chưa hoàn thành"));
                                meta.setTextColor(ContextCompat.getColor(TodayMealPlanActivity.this,
                                        isChecked ? R.color.primary_green : R.color.text_secondary));
                                updateProgressSummary(allPlans);
                                showMessage(isChecked ? "Đã đánh dấu hoàn thành" : "Đã bỏ đánh dấu");
                            }

                            @Override
                            public void onError(Exception e) {
                                showMessage("Lỗi cập nhật: " + e.getMessage());
                            }

                            @Override
                            public void onLoginRequired() {
                                showMessage("Vui lòng đăng nhập");
                            }
                        }));
        content.addView(checkBox);
        content.addView(meta);
        card.addView(content);
        return card;
    }

    private void updateProgressSummary(List<MealPlan> plans) {
        int total = plans != null ? plans.size() : 0;
        int completed = 0;
        if (plans != null) {
            for (MealPlan plan : plans) {
                if (plan.getCompleted() != null && plan.getCompleted()) {
                    completed++;
                }
            }
        }

        int percent = total == 0 ? 0 : Math.round((completed * 100f) / total);
        progressTodayCompletion.setProgress(percent);
        textTodayPlanProgress.setText("Đã hoàn thành " + completed + "/" + total + " món");
        textTodayPlanSummary.setText(percent == 100
                ? "Tuyệt vời, bạn đã hoàn thành kế hoạch hôm nay."
                : "Còn " + Math.max(total - completed, 0) + " món cần nấu trong hôm nay.");
    }

    private String formatMealType(String mealType) {
        if ("breakfast".equals(mealType)) {
            return "Bữa sáng";
        } else if ("lunch".equals(mealType)) {
            return "Bữa trưa";
        } else if ("dinner".equals(mealType)) {
            return "Bữa tối";
        }
        return "Bữa ăn";
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            layoutPlans.setVisibility(View.GONE);
            layoutState.setVisibility(View.GONE);
        }
    }

    private void showState(String message) {
        textState.setText(message);
        layoutPlans.setVisibility(View.GONE);
        layoutState.setVisibility(View.VISIBLE);
        updateProgressSummary(null);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showMessage(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }
}
