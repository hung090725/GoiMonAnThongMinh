package hung.edu.mealmindai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.models.User;
import hung.edu.mealmindai.repositories.FavoriteRepository;
import hung.edu.mealmindai.repositories.MealPlanRepository;
import hung.edu.mealmindai.repositories.RatingRepository;
import hung.edu.mealmindai.repositories.RecipeRepository;
import hung.edu.mealmindai.repositories.UserRepository;
import hung.edu.mealmindai.utils.RecentRecipeStore;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "recipeId";

    private ImageView imageRecipe;
    private ImageView buttonFavorite;
    private View appBar;
    private NestedScrollView detailScrollView;
    private LinearLayout sectionIngredients, sectionSteps;
    private TextView textName, textDescription, textCalories, textTime, textCost;
    private TextView textDifficulty, textAuthor, textLikes, textIngredients, textSteps, textDetailError;
    private TextView textNutritionNote, textAverageRating;
    private TextView buttonScrollIngredients, buttonScrollSteps, buttonScrollTop;
    private MaterialButton buttonAddTodayPlan, buttonOpenShoppingList, buttonOpenCookMode, buttonShareRecipe;
    private RatingBar ratingRecipe;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    // Favorite state
    private FavoriteRepository favoriteRepository;
    private MealPlanRepository mealPlanRepository;
    private RatingRepository ratingRepository;
    private UserRepository userRepository;
    private boolean isFavorited = false;
    private boolean isApplyingRating = false;
    private String currentFavoriteId = null;
    private String currentRecipeId = null;
    private Recipe currentRecipe = null;
    private String currentHealthGoal = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        db = FirebaseFirestore.getInstance();
        favoriteRepository = new FavoriteRepository();
        mealPlanRepository = new MealPlanRepository();
        ratingRepository = new RatingRepository();
        userRepository = new UserRepository();
        initViews();
        setupToolbar();
        setupQuickScrollButtons();
        setupFavoriteButton();
        setupMealPlanButton();
        setupRatingBar();
        setupAdvancedActionButtons();
        loadHealthGoal();

        currentRecipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (!TextUtils.isEmpty(currentRecipeId)) {
            loadRecipeDetails(currentRecipeId);
            checkFavoriteStatus(currentRecipeId);
            loadRatingInfo(currentRecipeId);
        } else {
            Toast.makeText(this, R.string.error_load_recipe, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        appBar = findViewById(R.id.appBar);
        imageRecipe = findViewById(R.id.imageRecipeDetail);
        buttonFavorite = findViewById(R.id.buttonFavorite);
        detailScrollView = findViewById(R.id.detailScrollView);
        sectionIngredients = findViewById(R.id.sectionIngredients);
        sectionSteps = findViewById(R.id.sectionSteps);
        textName = findViewById(R.id.textRecipeName);
        textDescription = findViewById(R.id.textRecipeDescription);
        textCalories = findViewById(R.id.textDetailCalories);
        textTime = findViewById(R.id.textDetailTime);
        textCost = findViewById(R.id.textDetailCost);
        textDifficulty = findViewById(R.id.textDetailDifficulty);
        textAuthor = findViewById(R.id.textDetailAuthor);
        textLikes = findViewById(R.id.textDetailLikes);
        textIngredients = findViewById(R.id.textIngredients);
        textSteps = findViewById(R.id.textSteps);
        textDetailError = findViewById(R.id.textDetailError);
        textNutritionNote = findViewById(R.id.textNutritionNote);
        textAverageRating = findViewById(R.id.textAverageRating);
        ratingRecipe = findViewById(R.id.ratingRecipe);
        buttonScrollIngredients = findViewById(R.id.buttonScrollIngredients);
        buttonScrollSteps = findViewById(R.id.buttonScrollSteps);
        buttonScrollTop = findViewById(R.id.buttonScrollTop);
        buttonAddTodayPlan = findViewById(R.id.buttonAddTodayPlan);
        buttonOpenShoppingList = findViewById(R.id.buttonOpenShoppingList);
        buttonOpenCookMode = findViewById(R.id.buttonOpenCookMode);
        buttonShareRecipe = findViewById(R.id.buttonShareRecipe);
        progressBar = findViewById(R.id.progressDetail);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupQuickScrollButtons() {
        buttonScrollIngredients.setOnClickListener(v -> smoothScrollToSection(sectionIngredients));
        buttonScrollSteps.setOnClickListener(v -> smoothScrollToSection(sectionSteps));
        buttonScrollTop.setOnClickListener(v -> detailScrollView.smoothScrollTo(0, 0));
    }

    private void loadHealthGoal() {
        userRepository.getCurrentUserProfile(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentHealthGoal = user != null && user.getHealthGoal() != null
                        ? user.getHealthGoal() : "";
                updateNutritionNote();
            }

            @Override
            public void onError(Exception e) {
                currentHealthGoal = "";
                updateNutritionNote();
            }
        });
    }

    private void showMessage(String message) {
        View root = detailScrollView != null ? detailScrollView : imageRecipe;
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }

    // -------------------------------------------------------------------------
    // Favorite Logic
    // -------------------------------------------------------------------------

    private void setupFavoriteButton() {
        buttonFavorite.setOnClickListener(v -> {
            // Kiểm tra đăng nhập
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                showMessage(getString(R.string.favorite_login_required));
                return;
            }

            if (TextUtils.isEmpty(currentRecipeId)) {
                return;
            }

            // Disable nút tạm thời để tránh bấm nhiều lần
            buttonFavorite.setEnabled(false);

            if (isFavorited) {
                // Bỏ yêu thích
                FavoriteRepository.FavoriteActionCallback removeCallback = new FavoriteRepository.FavoriteActionCallback() {
                    @Override
                    public void onSuccess() {
                        isFavorited = false;
                        currentFavoriteId = null;
                        updateFavoriteIcon();
                        buttonFavorite.setEnabled(true);
                        showMessage(getString(R.string.favorite_removed));
                    }

                    @Override
                    public void onError(Exception e) {
                        buttonFavorite.setEnabled(true);
                        showMessage("Lỗi: " + e.getMessage());
                    }
                };

                if (TextUtils.isEmpty(currentFavoriteId)) {
                    favoriteRepository.removeFavoriteByRecipeId(currentRecipeId, removeCallback);
                } else {
                    favoriteRepository.removeFavorite(currentFavoriteId, removeCallback);
                }
            } else {
                // Thêm yêu thích
                favoriteRepository.addFavorite(currentRecipeId, new FavoriteRepository.FavoriteActionCallback() {
                    @Override
                    public void onSuccess() {
                        isFavorited = true;
                        // Kiểm tra lại để lấy favoriteId mới
                        checkFavoriteStatus(currentRecipeId);
                        updateFavoriteIcon();
                        buttonFavorite.setEnabled(true);
                        showMessage(getString(R.string.favorite_added));
                    }

                    @Override
                    public void onError(Exception e) {
                        buttonFavorite.setEnabled(true);
                        showMessage("Lỗi: " + e.getMessage());
                    }
                });
            }
        });
    }

    private void setupMealPlanButton() {
        buttonAddTodayPlan.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                showMessage("Vui lòng đăng nhập để lập kế hoạch bữa ăn");
                return;
            }
            if (currentRecipe == null || TextUtils.isEmpty(currentRecipeId)) {
                showMessage("Chưa có dữ liệu món ăn");
                return;
            }
            showMealTypeDialog();
        });
    }

    private void setupAdvancedActionButtons() {
        buttonShareRecipe.setOnClickListener(v -> shareRecipe());

        buttonOpenShoppingList.setOnClickListener(v -> {
            if (TextUtils.isEmpty(currentRecipeId)) {
                showMessage("Chưa có dữ liệu món ăn");
                return;
            }
            Intent intent = new Intent(this, ShoppingListActivity.class);
            intent.putExtra(ShoppingListActivity.EXTRA_RECIPE_ID, currentRecipeId);
            startActivity(intent);
        });

        buttonOpenCookMode.setOnClickListener(v -> {
            if (TextUtils.isEmpty(currentRecipeId)) {
                showMessage("Chưa có dữ liệu món ăn");
                return;
            }
            Intent intent = new Intent(this, CookModeActivity.class);
            intent.putExtra(CookModeActivity.EXTRA_RECIPE_ID, currentRecipeId);
            startActivity(intent);
        });
    }

    private void setupRatingBar() {
        ratingRecipe.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (!fromUser || isApplyingRating || TextUtils.isEmpty(currentRecipeId)) {
                return;
            }

            ratingRepository.saveRating(currentRecipeId, rating, new RatingRepository.ActionCallback() {
                @Override
                public void onSuccess(double averageRating) {
                    updateAverageRatingText(averageRating);
                    showMessage("Đã lưu đánh giá " + Math.round(rating) + " sao");
                }

                @Override
                public void onError(Exception e) {
                    showMessage("Lỗi lưu đánh giá: " + e.getMessage());
                }

                @Override
                public void onLoginRequired() {
                    showMessage("Vui lòng đăng nhập để đánh giá món ăn");
                }
            });
        });
    }

    private void loadRatingInfo(String recipeId) {
        ratingRepository.loadRatingInfo(recipeId, new RatingRepository.RatingCallback() {
            @Override
            public void onSuccess(float userRating, double averageRating) {
                isApplyingRating = true;
                ratingRecipe.setRating(userRating);
                isApplyingRating = false;
                updateAverageRatingText(averageRating);
            }

            @Override
            public void onError(Exception e) {
                updateAverageRatingText(0);
            }

            @Override
            public void onLoginRequired() {
                textAverageRating.setText("Đăng nhập để đánh giá món ăn");
            }
        });
    }

    private void updateAverageRatingText(double averageRating) {
        if (averageRating <= 0) {
            textAverageRating.setText("Chưa có đánh giá");
            return;
        }
        textAverageRating.setText(String.format(Locale.getDefault(), "⭐ %.1f điểm đánh giá trung bình", averageRating));
    }

    private void shareRecipe() {
        if (currentRecipe == null) {
            showMessage("Chưa có dữ liệu công thức để chia sẻ");
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Món ăn: ")
                .append(currentRecipe.getName() != null ? currentRecipe.getName() : "Công thức MealMind AI")
                .append("\n\nNguyên liệu: ")
                .append(formatIngredientsForShare(currentRecipe.getIngredients()))
                .append("\nThời gian: ")
                .append(currentRecipe.getCookingTime() != null ? currentRecipe.getCookingTime() : 0)
                .append(" phút")
                .append("\nChi phí: ")
                .append(currentRecipe.getEstimatedCost() != null
                        ? NumberFormat.getInstance(new Locale("vi", "VN")).format(currentRecipe.getEstimatedCost()) + "đ"
                        : "Đang cập nhật")
                .append("\nCalo: ")
                .append(currentRecipe.getCalories() != null ? currentRecipe.getCalories() + " kcal" : "Đang cập nhật")
                .append("\n\nMealMind AI - Gợi ý món ăn từ nguyên liệu đang có.");

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Công thức " + currentRecipe.getName());
        sendIntent.putExtra(Intent.EXTRA_TITLE, "Chia sẻ công thức MealMind AI");
        sendIntent.putExtra(Intent.EXTRA_TEXT, builder.toString());
        startActivity(Intent.createChooser(sendIntent, "Chia sẻ công thức"));
    }

    private String formatIngredientsForShare(List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return "Đang cập nhật";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ingredients.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(ingredients.get(i));
        }
        return builder.toString();
    }

    private void showMealTypeDialog() {
        String[] labels = {"Bữa sáng", "Bữa trưa", "Bữa tối"};
        String[] values = {"breakfast", "lunch", "dinner"};

        new AlertDialog.Builder(this)
                .setTitle("Thêm vào kế hoạch hôm nay")
                .setItems(labels, (dialog, which) -> addRecipeToTodayPlan(values[which], labels[which]))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void addRecipeToTodayPlan(String mealType, String mealLabel) {
        buttonAddTodayPlan.setEnabled(false);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String recipeName = currentRecipe != null ? currentRecipe.getName() : "";

        mealPlanRepository.addRecipeToTodayPlan(
                currentRecipeId,
                recipeName,
                today,
                mealType,
                new MealPlanRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        buttonAddTodayPlan.setEnabled(true);
                        showMessage("Đã thêm vào kế hoạch " + mealLabel.toLowerCase(Locale.ROOT));
                    }

                    @Override
                    public void onError(Exception e) {
                        buttonAddTodayPlan.setEnabled(true);
                        showMessage("Lỗi kế hoạch: " + e.getMessage());
                    }

                    @Override
                    public void onLoginRequired() {
                        buttonAddTodayPlan.setEnabled(true);
                        showMessage("Vui lòng đăng nhập để lập kế hoạch bữa ăn");
                    }
                });
    }

    private void checkFavoriteStatus(String recipeId) {
        favoriteRepository.checkFavorite(recipeId, new FavoriteRepository.FavoriteCheckCallback() {
            @Override
            public void onResult(boolean favorited, String favoriteId) {
                isFavorited = favorited;
                currentFavoriteId = favoriteId;
                updateFavoriteIcon();
            }

            @Override
            public void onError(Exception e) {
                // Im lặng nếu lỗi kiểm tra — không ảnh hưởng UX
            }
        });
    }

    private void updateFavoriteIcon() {
        if (isFavorited) {
            buttonFavorite.setImageResource(R.drawable.ic_favorite_filled);
            buttonFavorite.setColorFilter(null); // Giữ nguyên màu đỏ của icon
        } else {
            buttonFavorite.setImageResource(R.drawable.ic_favorite_outline);
            buttonFavorite.setColorFilter(null); // Giữ nguyên màu xám của icon
        }
    }

    // -------------------------------------------------------------------------
    // Recipe Loading
    // -------------------------------------------------------------------------

    private void smoothScrollToSection(View targetView) {
        if (targetView == null || detailScrollView == null) {
            return;
        }
        detailScrollView.post(() -> detailScrollView.smoothScrollTo(0, Math.max(targetView.getTop() - 18, 0)));
    }

    private void loadRecipeDetails(String recipeId) {
        setLoading(true);
        db.collection("recipes").document(recipeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (documentSnapshot.exists()) {
                        try {
                            Recipe recipe = RecipeRepository.mapRecipeDocument(documentSnapshot);
                            if (recipe.getName() != null || recipe.getTitle() != null) {
                                displayRecipe(recipe);
                            }
                        } catch (Exception e) {
                            Log.e("RecipeDetail", "Error deserializing recipe: " + e.getMessage());
                            showErrorState(getString(R.string.error_load_recipe));
                        }
                    } else {
                        showErrorState(getString(R.string.error_load_recipe));
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showErrorState(getString(R.string.error_load_recipe));
                    showMessage(getString(R.string.error_load_recipe) + ": " + e.getMessage());
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        appBar.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        detailScrollView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        textDetailError.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        appBar.setVisibility(View.GONE);
        detailScrollView.setVisibility(View.GONE);
        textDetailError.setText(message);
        textDetailError.setVisibility(View.VISIBLE);
    }

    private void displayRecipe(Recipe recipe) {
        if (recipe == null) return;
        currentRecipe = recipe;
        RecentRecipeStore.saveViewedRecipe(this, currentRecipeId);
        appBar.setVisibility(View.VISIBLE);
        detailScrollView.setVisibility(View.VISIBLE);
        textDetailError.setVisibility(View.GONE);

        textName.setText(recipe.getName() != null ? recipe.getName() : "Không tên");
        textDescription.setText(recipe.getDescription() != null ? recipe.getDescription() : "");

        // Stats
        int calories = recipe.getCalories() != null ? recipe.getCalories() : 0;
        textCalories.setText(String.format(Locale.getDefault(), "%d %s",
                calories, getString(R.string.unit_kcal)));

        int time = recipe.getCookingTime() != null ? recipe.getCookingTime() : 0;
        textTime.setText(String.format(Locale.getDefault(), "%d %s",
                time, getString(R.string.unit_minutes)));

        double cost = recipe.getEstimatedCost() != null ? recipe.getEstimatedCost() : 0.0;
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        textCost.setText(String.format("%s%s",
                currencyFormat.format(cost), getString(R.string.unit_currency)));
        textDifficulty.setText(TextUtils.isEmpty(recipe.getDifficulty()) ? "Dễ" : recipe.getDifficulty());
        textAuthor.setText(TextUtils.isEmpty(recipe.getAuthorName())
                ? "MealMind Kitchen"
                : recipe.getAuthorName());
        int likeCount = recipe.getLikeCount() != null ? recipe.getLikeCount() : 0;
        textLikes.setText(String.format(Locale.getDefault(), "%d lượt thích", likeCount));

        // Ingredients
        StringBuilder ingredientsBuilder = new StringBuilder();
        List<String> ingredients = recipe.getIngredients();
        if (ingredients != null && !ingredients.isEmpty()) {
            for (String item : ingredients) {
                ingredientsBuilder.append("• ").append(item).append("\n");
            }
        } else {
            ingredientsBuilder.append("Đang cập nhật nguyên liệu...");
        }
        textIngredients.setText(ingredientsBuilder.toString().trim());

        // Steps
        StringBuilder stepsBuilder = new StringBuilder();
        List<String> steps = recipe.getSteps();
        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                stepsBuilder.append("Bước ")
                        .append(i + 1)
                        .append(": ")
                        .append(steps.get(i))
                        .append("\n\n");
            }
        } else {
            stepsBuilder.append("Đang cập nhật cách chế biến...");
        }
        textSteps.setText(stepsBuilder.toString().trim());

        loadRecipeImage(recipe);
        updateNutritionNote();
    }

    private void updateNutritionNote() {
        if (textNutritionNote == null || currentRecipe == null) {
            return;
        }

        int calories = currentRecipe.getCalories() != null ? currentRecipe.getCalories() : 0;
        String note;
        if ("Giảm cân".equals(currentHealthGoal)) {
            note = calories > 0 && calories <= 450
                    ? "Mức phù hợp sức khỏe: phù hợp mục tiêu giảm cân ở mức cơ bản."
                    : "Mức phù hợp sức khỏe: calo hơi cao, nên cân nhắc khẩu phần khi giảm cân.";
        } else if ("Tăng cân".equals(currentHealthGoal)) {
            note = calories >= 450
                    ? "Mức phù hợp sức khỏe: phù hợp mục tiêu tăng cân ở mức cơ bản."
                    : "Mức phù hợp sức khỏe: món khá nhẹ, có thể bổ sung thêm khẩu phần.";
        } else if ("Ăn tiết kiệm".equals(currentHealthGoal)) {
            note = "Mức phù hợp sức khỏe: ưu tiên chi phí và vẫn hiển thị calo cơ bản.";
        } else if ("Ăn nhanh".equals(currentHealthGoal)) {
            note = "Mức phù hợp sức khỏe: phù hợp khi cần chọn món nhanh, dễ chuẩn bị.";
        } else {
            note = "Mức phù hợp sức khỏe: phù hợp ở mức cơ bản, chưa phân tích macro chuyên sâu.";
        }
        textNutritionNote.setText(note);
    }

    private void loadRecipeImage(Recipe recipe) {
        String imageUrl = recipe.getImageUrl();
        Object imageSource = R.drawable.ic_meal_placeholder;

        int localImageRes = getLocalImageForRecipe(recipe);
        if (localImageRes != 0) {
            imageSource = localImageRes;
        } else if (!TextUtils.isEmpty(imageUrl)) {
            if (imageUrl.startsWith("res://")) {
                String resName = imageUrl.replace("res://", "");
                int resId = getResources().getIdentifier(resName, "drawable", getPackageName());
                imageSource = resId != 0 ? resId : R.drawable.ic_meal_placeholder;
            } else {
                imageSource = imageUrl;
            }
        }

        Glide.with(this)
                .load(imageSource)
                .placeholder(R.drawable.ic_meal_placeholder)
                .error(R.drawable.ic_meal_placeholder)
                .centerCrop()
                .into(imageRecipe);
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
}
