package hung.edu.mealmindai.activities;

import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DecimalFormat;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.AdminRecipeRepository;

public class PendingRecipeDetailActivity extends AppCompatActivity {
    public static final String EXTRA_RECIPE_ID = "recipeId";

    private ImageView imageRecipe;
    private TextView textName, textDescription, textStats, textAuthor, textStatus;
    private TextView textIngredients, textSteps, textModerationMessage, textError;
    private MaterialButton buttonApprove, buttonReject;
    private ProgressBar progressBar;
    private View contentView;
    private AdminRecipeRepository repository;
    private String recipeId;
    private boolean actionInProgress = false;
    private final DecimalFormat costFormat = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_recipe_detail);

        repository = new AdminRecipeRepository();
        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (TextUtils.isEmpty(recipeId)) {
            Toast.makeText(this, "Không tìm thấy mã món ăn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        checkAdminAndLoad();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        imageRecipe = findViewById(R.id.imagePendingDetail);
        textName = findViewById(R.id.textPendingDetailName);
        textDescription = findViewById(R.id.textPendingDetailDescription);
        textStats = findViewById(R.id.textPendingDetailStats);
        textAuthor = findViewById(R.id.textPendingDetailAuthor);
        textStatus = findViewById(R.id.textPendingDetailStatus);
        textIngredients = findViewById(R.id.textPendingDetailIngredients);
        textSteps = findViewById(R.id.textPendingDetailSteps);
        textModerationMessage = findViewById(R.id.textModerationMessage);
        textError = findViewById(R.id.textPendingDetailError);
        buttonApprove = findViewById(R.id.buttonApproveRecipe);
        buttonReject = findViewById(R.id.buttonRejectRecipe);
        progressBar = findViewById(R.id.progressPendingDetail);
        contentView = findViewById(R.id.pendingDetailContent);

        buttonApprove.setOnClickListener(v -> confirmApprove());
        buttonReject.setOnClickListener(v -> showRejectDialog());
    }

    private void checkAdminAndLoad() {
        repository.checkCurrentUserIsAdmin(new AdminRecipeRepository.AdminCheckCallback() {
            @Override
            public void onResult(boolean isAdmin) {
                if (!isAdmin) {
                    Toast.makeText(PendingRecipeDetailActivity.this,
                            "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                        goToLogin();
                    } else {
                        finish();
                    }
                    return;
                }
                loadRecipe();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(PendingRecipeDetailActivity.this,
                        "Lỗi kiểm tra quyền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void loadRecipe() {
        setLoading(true);
        repository.loadRecipeById(recipeId, new AdminRecipeRepository.RecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                setLoading(false);
                if (recipes == null || recipes.isEmpty()) {
                    showError("Món ăn không tồn tại");
                    return;
                }
                bindRecipe(recipes.get(0));
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                showError("Lỗi tải món: " + e.getMessage());
            }
        });
    }

    private void bindRecipe(Recipe recipe) {
        contentView.setVisibility(View.VISIBLE);
        textError.setVisibility(View.GONE);

        textName.setText(emptyToDefault(recipe.getName(), "Món chưa đặt tên"));
        textDescription.setText(emptyToDefault(recipe.getDescription(), "Chưa có mô tả"));
        int calories = recipe.getCalories() != null ? recipe.getCalories() : 0;
        double cost = recipe.getEstimatedCost() != null ? recipe.getEstimatedCost() : 0;
        int time = recipe.getCookingTime() != null ? recipe.getCookingTime() : 0;
        String difficulty = emptyToDefault(recipe.getDifficulty(), "Dễ");
        textStats.setText(calories + " kcal • " + costFormat.format(cost).replace(",", ".")
                + "đ • " + time + " phút • " + difficulty);
        textAuthor.setText("Tác giả: " + emptyToDefault(recipe.getAuthorName(), "Người dùng MealMind"));
        textStatus.setText("Trạng thái: " + emptyToDefault(recipe.getStatus(), "pending"));
        textIngredients.setText(formatLines(recipe.getIngredients(), true));
        textSteps.setText(formatLines(recipe.getSteps(), false));

        Glide.with(this)
                .load(TextUtils.isEmpty(recipe.getImageUrl()) ? R.drawable.ic_meal_placeholder : recipe.getImageUrl())
                .placeholder(R.drawable.ic_meal_placeholder)
                .error(R.drawable.ic_meal_placeholder)
                .centerCrop()
                .into(imageRecipe);

        boolean isPending = "pending".equalsIgnoreCase(recipe.getStatus());
        buttonApprove.setEnabled(isPending);
        buttonReject.setEnabled(isPending);
        textModerationMessage.setVisibility(isPending ? View.GONE : View.VISIBLE);
    }

    private void confirmApprove() {
        new AlertDialog.Builder(this)
                .setTitle("Duyệt món ăn")
                .setMessage("Món này sẽ hiển thị ở Home và Search sau khi duyệt.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Duyệt", (dialog, which) -> approveRecipe())
                .show();
    }

    private void approveRecipe() {
        if (actionInProgress) {
            return;
        }
        setActionLoading(true);
        repository.approveRecipe(recipeId, new AdminRecipeRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(PendingRecipeDetailActivity.this, "Đã duyệt món", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                setActionLoading(false);
                Toast.makeText(PendingRecipeDetailActivity.this,
                        "Lỗi duyệt món: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRejectDialog() {
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint("Lý do từ chối");
        TextInputEditText input = new TextInputEditText(this);
        input.setMinLines(2);
        inputLayout.addView(input);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        inputLayout.setPadding(padding, 0, padding, 0);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Từ chối món ăn")
                .setView(inputLayout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Từ chối", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String reason = input.getText() != null ? input.getText().toString().trim() : "";
            if (TextUtils.isEmpty(reason)) {
                inputLayout.setError("Vui lòng nhập lý do từ chối");
                return;
            }
            dialog.dismiss();
            rejectRecipe(reason);
        }));
        dialog.show();
    }

    private void rejectRecipe(String reason) {
        if (actionInProgress) {
            return;
        }
        setActionLoading(true);
        repository.rejectRecipe(recipeId, reason, new AdminRecipeRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(PendingRecipeDetailActivity.this, "Đã từ chối món", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                setActionLoading(false);
                Toast.makeText(PendingRecipeDetailActivity.this,
                        "Lỗi từ chối món: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        contentView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        textError.setVisibility(View.GONE);
    }

    private void setActionLoading(boolean isLoading) {
        actionInProgress = isLoading;
        buttonApprove.setEnabled(!isLoading);
        buttonReject.setEnabled(!isLoading);
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        contentView.setVisibility(View.GONE);
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }

    private String emptyToDefault(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String formatLines(List<String> values, boolean bullet) {
        if (values == null || values.isEmpty()) {
            return "Đang cập nhật";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (bullet) {
                builder.append("• ");
            } else {
                builder.append(i + 1).append(". ");
            }
            builder.append(values.get(i)).append("\n");
        }
        return builder.toString().trim();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
