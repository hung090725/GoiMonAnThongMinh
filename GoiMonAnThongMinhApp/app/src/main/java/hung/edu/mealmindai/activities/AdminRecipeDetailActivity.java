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
import com.google.firebase.auth.FirebaseAuth;

import java.text.DecimalFormat;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.AdminRecipeRepository;

public class AdminRecipeDetailActivity extends AppCompatActivity {
    public static final String EXTRA_RECIPE_ID = "recipeId";

    private AdminRecipeRepository repository;
    private String recipeId;
    private ImageView imageRecipe;
    private TextView textName, textDescription, textStats, textAuthor, textStatus;
    private TextView textReviewInfo, textRejectReason, textIngredients, textSteps, textError;
    private MaterialButton buttonPrimaryAction, buttonPendingAction;
    private ProgressBar progressBar;
    private View contentView;
    private Recipe currentRecipe;
    private boolean actionInProgress = false;
    private final DecimalFormat costFormat = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_recipe_detail);

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

        imageRecipe = findViewById(R.id.imageAdminDetail);
        textName = findViewById(R.id.textAdminDetailName);
        textDescription = findViewById(R.id.textAdminDetailDescription);
        textStats = findViewById(R.id.textAdminDetailStats);
        textAuthor = findViewById(R.id.textAdminDetailAuthor);
        textStatus = findViewById(R.id.textAdminDetailStatus);
        textReviewInfo = findViewById(R.id.textAdminDetailReviewInfo);
        textRejectReason = findViewById(R.id.textAdminDetailRejectReason);
        textIngredients = findViewById(R.id.textAdminDetailIngredients);
        textSteps = findViewById(R.id.textAdminDetailSteps);
        textError = findViewById(R.id.textAdminDetailError);
        buttonPrimaryAction = findViewById(R.id.buttonAdminRecipePrimaryAction);
        buttonPendingAction = findViewById(R.id.buttonAdminRecipePendingAction);
        progressBar = findViewById(R.id.progressAdminDetail);
        contentView = findViewById(R.id.adminDetailContent);
    }

    private void checkAdminAndLoad() {
        repository.checkCurrentUserIsAdmin(new AdminRecipeRepository.AdminCheckCallback() {
            @Override
            public void onResult(boolean isAdmin) {
                if (!isAdmin) {
                    Toast.makeText(AdminRecipeDetailActivity.this,
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
                Toast.makeText(AdminRecipeDetailActivity.this,
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
                currentRecipe = recipes.get(0);
                bindRecipe(currentRecipe);
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
        textName.setText(emptyToDefault(recipe.getName(), "Món ăn"));
        textDescription.setText(emptyToDefault(recipe.getDescription(), "Chưa có mô tả"));
        int calories = recipe.getCalories() != null ? recipe.getCalories() : 0;
        double cost = recipe.getEstimatedCost() != null ? recipe.getEstimatedCost() : 0;
        int time = recipe.getCookingTime() != null ? recipe.getCookingTime() : 0;
        textStats.setText(calories + " kcal • " + costFormat.format(cost).replace(",", ".")
                + "đ • " + time + " phút • " + emptyToDefault(recipe.getDifficulty(), "Dễ"));
        textAuthor.setText("Tác giả: " + emptyToDefault(recipe.getAuthorName(), "MealMind"));
        textStatus.setText("Trạng thái: " + emptyToDefault(recipe.getStatus(), "pending"));
        textReviewInfo.setText("Người duyệt: " + emptyToDefault(recipe.getReviewedBy(), "Chưa có"));
        textRejectReason.setText("Lý do từ chối: " + emptyToDefault(recipe.getRejectReason(), "Không có"));
        textRejectReason.setVisibility("rejected".equalsIgnoreCase(recipe.getStatus()) ? View.VISIBLE : View.GONE);
        textIngredients.setText(formatLines(recipe.getIngredients(), true));
        textSteps.setText(formatLines(recipe.getSteps(), false));

        Glide.with(this)
                .load(resolveRecipeImage(recipe))
                .placeholder(R.drawable.ic_meal_placeholder)
                .error(R.drawable.ic_meal_placeholder)
                .centerCrop()
                .into(imageRecipe);

        setupActions(recipe.getStatus());
    }

    private Object resolveRecipeImage(Recipe recipe) {
        int localImage = getLocalImageForRecipe(recipe);
        if (localImage != 0) {
            return localImage;
        }

        String imageUrl = recipe.getImageUrl();
        if (TextUtils.isEmpty(imageUrl)) {
            return R.drawable.ic_meal_placeholder;
        }

        if (imageUrl.startsWith("res://")) {
            String resName = imageUrl.replace("res://", "");
            int resId = getResources().getIdentifier(resName, "drawable", getPackageName());
            return resId != 0 ? resId : R.drawable.ic_meal_placeholder;
        }

        return imageUrl;
    }

    private int getLocalImageForRecipe(Recipe recipe) {
        String name = recipe != null ? recipe.getName() : null;
        if (TextUtils.isEmpty(name)) {
            return 0;
        }

        String normalizedName = name.toLowerCase();
        if (normalizedName.contains("gà xào sả ớt")) {
            return R.drawable.gaxaosa;
        } else if (normalizedName.contains("trứng sốt cà")) {
            return R.drawable.trungsotcachua;
        } else if (normalizedName.contains("cháo thịt băm")) {
            return R.drawable.chaobam;
        } else if (normalizedName.contains("đậu hũ sốt cà")) {
            return R.drawable.dauhuca;
        }
        return 0;
    }

    private void setupActions(String status) {
        buttonPrimaryAction.setVisibility(View.GONE);
        buttonPendingAction.setVisibility(View.GONE);
        buttonPrimaryAction.setEnabled(true);
        buttonPendingAction.setEnabled(true);

        if ("approved".equalsIgnoreCase(status)) {
            buttonPrimaryAction.setVisibility(View.VISIBLE);
            buttonPrimaryAction.setText("Ẩn món");
            buttonPrimaryAction.setOnClickListener(v -> confirmStatusChange("Ẩn món", "Món này sẽ không còn hiện với người dùng.", true));
        } else if ("hidden".equalsIgnoreCase(status)) {
            buttonPrimaryAction.setVisibility(View.VISIBLE);
            buttonPrimaryAction.setText("Khôi phục");
            buttonPrimaryAction.setOnClickListener(v -> confirmStatusChange("Khôi phục món", "Món này sẽ hiện lại trên Home/Search.", false));
        } else if ("pending".equalsIgnoreCase(status)) {
            buttonPendingAction.setVisibility(View.VISIBLE);
            buttonPendingAction.setText("Mở màn duyệt");
            buttonPendingAction.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, PendingRecipeDetailActivity.class);
                intent.putExtra(PendingRecipeDetailActivity.EXTRA_RECIPE_ID, recipeId);
                startActivity(intent);
            });
        }
    }

    private void confirmStatusChange(String title, String message, boolean hide) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    if (hide) {
                        hideRecipe();
                    } else {
                        restoreRecipe();
                    }
                })
                .show();
    }

    private void hideRecipe() {
        if (actionInProgress) {
            return;
        }
        setActionLoading(true);
        repository.hideRecipe(recipeId, new AdminRecipeRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AdminRecipeDetailActivity.this, "Đã ẩn món", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                setActionLoading(false);
                Toast.makeText(AdminRecipeDetailActivity.this, "Lỗi ẩn món: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void restoreRecipe() {
        if (actionInProgress) {
            return;
        }
        setActionLoading(true);
        repository.restoreRecipe(recipeId, new AdminRecipeRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AdminRecipeDetailActivity.this, "Đã khôi phục món", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                setActionLoading(false);
                Toast.makeText(AdminRecipeDetailActivity.this, "Lỗi khôi phục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonPrimaryAction.setEnabled(!isLoading);
        buttonPendingAction.setEnabled(!isLoading);
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
            builder.append(bullet ? "• " : (i + 1) + ". ");
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
