package hung.edu.mealmindai.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.RecipeRepository;

public class CookModeActivity extends AppCompatActivity {
    public static final String EXTRA_RECIPE_ID = "recipeId";

    private static final long TIMER_MILLIS = 5 * 60 * 1000L;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<String> steps = new ArrayList<>();
    private int currentStepIndex = 0;
    private CountDownTimer timer;
    private long remainingMillis = TIMER_MILLIS;

    private TextView textRecipeName;
    private TextView textProgress;
    private TextView textStep;
    private TextView textTimer;
    private MaterialButton buttonPrev;
    private MaterialButton buttonNext;
    private MaterialButton buttonTimerStart;
    private MaterialButton buttonTimerReset;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cook_mode);

        initViews();
        setupClicks();

        String recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (TextUtils.isEmpty(recipeId)) {
            Toast.makeText(this, "Thiếu mã món ăn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadRecipe(recipeId);
    }

    @Override
    protected void onDestroy() {
        cancelTimer();
        super.onDestroy();
    }

    private void initViews() {
        ImageButton buttonBack = findViewById(R.id.buttonCookBack);
        textRecipeName = findViewById(R.id.textCookRecipeName);
        textProgress = findViewById(R.id.textCookProgress);
        textStep = findViewById(R.id.textCookStep);
        textTimer = findViewById(R.id.textCookTimer);
        buttonPrev = findViewById(R.id.buttonCookPrev);
        buttonNext = findViewById(R.id.buttonCookNext);
        buttonTimerStart = findViewById(R.id.buttonCookTimerStart);
        buttonTimerReset = findViewById(R.id.buttonCookTimerReset);
        progressBar = findViewById(R.id.progressCookMode);
        buttonBack.setOnClickListener(v -> finish());
        updateTimerText();
    }

    private void setupClicks() {
        buttonPrev.setOnClickListener(v -> {
            if (currentStepIndex > 0) {
                currentStepIndex--;
                renderStep();
            }
        });

        buttonNext.setOnClickListener(v -> {
            if (currentStepIndex < steps.size() - 1) {
                currentStepIndex++;
                renderStep();
            } else {
                Toast.makeText(this, "Bạn đã hoàn thành các bước nấu", Toast.LENGTH_SHORT).show();
            }
        });

        buttonTimerStart.setOnClickListener(v -> startTimer());
        buttonTimerReset.setOnClickListener(v -> resetTimer());
    }

    private void loadRecipe(String recipeId) {
        setLoading(true);
        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Không tìm thấy món ăn", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Recipe recipe = RecipeRepository.mapRecipeDocument(documentSnapshot);
                    textRecipeName.setText(TextUtils.isEmpty(recipe.getName()) ? "Cook Mode" : recipe.getName());
                    steps.clear();
                    if (recipe.getSteps() != null) {
                        for (String step : recipe.getSteps()) {
                            if (!TextUtils.isEmpty(step)) {
                                steps.add(step.trim());
                            }
                        }
                    }
                    if (steps.isEmpty()) {
                        steps.add("Công thức này đang cập nhật các bước nấu.");
                    }
                    currentStepIndex = 0;
                    renderStep();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi tải Cook Mode: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void renderStep() {
        textProgress.setText(String.format(Locale.getDefault(),
                "Bước %d/%d", currentStepIndex + 1, steps.size()));
        textStep.setText("Bước " + (currentStepIndex + 1) + "\n\n" + steps.get(currentStepIndex));
        buttonPrev.setEnabled(currentStepIndex > 0);
        buttonNext.setText(currentStepIndex == steps.size() - 1 ? "Hoàn thành" : "Bước tiếp");
    }

    private void startTimer() {
        cancelTimer();
        buttonTimerStart.setEnabled(false);
        timer = new CountDownTimer(remainingMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                remainingMillis = 0;
                updateTimerText();
                buttonTimerStart.setEnabled(true);
                Toast.makeText(CookModeActivity.this, "Đã hết 5 phút", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void resetTimer() {
        cancelTimer();
        remainingMillis = TIMER_MILLIS;
        buttonTimerStart.setEnabled(true);
        updateTimerText();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void updateTimerText() {
        long totalSeconds = remainingMillis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        textTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
