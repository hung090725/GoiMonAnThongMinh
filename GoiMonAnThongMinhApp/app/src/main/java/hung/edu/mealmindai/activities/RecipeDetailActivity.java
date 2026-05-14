package hung.edu.mealmindai.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.FavoriteRepository;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "recipeId";

    private ImageView imageRecipe;
    private ImageView buttonFavorite;
    private View appBar;
    private NestedScrollView detailScrollView;
    private LinearLayout sectionIngredients, sectionSteps;
    private TextView textName, textDescription, textCalories, textTime, textCost;
    private TextView textDifficulty, textAuthor, textLikes, textIngredients, textSteps, textDetailError;
    private TextView buttonScrollIngredients, buttonScrollSteps, buttonScrollTop;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    // Favorite state
    private FavoriteRepository favoriteRepository;
    private boolean isFavorited = false;
    private String currentFavoriteId = null;
    private String currentRecipeId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        db = FirebaseFirestore.getInstance();
        favoriteRepository = new FavoriteRepository();
        initViews();
        setupToolbar();
        setupQuickScrollButtons();
        setupFavoriteButton();

        currentRecipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (!TextUtils.isEmpty(currentRecipeId)) {
            loadRecipeDetails(currentRecipeId);
            checkFavoriteStatus(currentRecipeId);
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
        buttonScrollIngredients = findViewById(R.id.buttonScrollIngredients);
        buttonScrollSteps = findViewById(R.id.buttonScrollSteps);
        buttonScrollTop = findViewById(R.id.buttonScrollTop);
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

    // -------------------------------------------------------------------------
    // Favorite Logic
    // -------------------------------------------------------------------------

    private void setupFavoriteButton() {
        buttonFavorite.setOnClickListener(v -> {
            // Kiểm tra đăng nhập
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, R.string.favorite_login_required, Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(currentRecipeId)) {
                return;
            }

            // Disable nút tạm thời để tránh bấm nhiều lần
            buttonFavorite.setEnabled(false);

            if (isFavorited) {
                // Bỏ yêu thích
                favoriteRepository.removeFavorite(currentFavoriteId, new FavoriteRepository.FavoriteActionCallback() {
                    @Override
                    public void onSuccess() {
                        isFavorited = false;
                        currentFavoriteId = null;
                        updateFavoriteIcon();
                        buttonFavorite.setEnabled(true);
                        Toast.makeText(RecipeDetailActivity.this, R.string.favorite_removed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        buttonFavorite.setEnabled(true);
                        Toast.makeText(RecipeDetailActivity.this,
                                "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
                        Toast.makeText(RecipeDetailActivity.this, R.string.favorite_added, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        buttonFavorite.setEnabled(true);
                        Toast.makeText(RecipeDetailActivity.this,
                                "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
                        Recipe recipe = mapDocumentToRecipe(documentSnapshot);
                        displayRecipe(recipe);
                    } else {
                        showErrorState(getString(R.string.error_load_recipe));
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showErrorState(getString(R.string.error_load_recipe));
                    Toast.makeText(this,
                            getString(R.string.error_load_recipe) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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

    private Recipe mapDocumentToRecipe(com.google.firebase.firestore.DocumentSnapshot document) {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(document.getId());
        recipe.setName(document.getString("name"));
        recipe.setDescription(document.getString("description"));
        recipe.setImageUrl(document.getString("imageUrl"));

        Object caloriesObj = document.get("calories");
        recipe.setCalories(caloriesObj instanceof Number ? ((Number) caloriesObj).intValue() : 0);

        Object costObj = document.get("estimatedCost");
        recipe.setEstimatedCost(costObj instanceof Number ? ((Number) costObj).doubleValue() : 0.0);

        Object timeObj = document.get("cookingTime");
        recipe.setCookingTime(timeObj instanceof Number ? ((Number) timeObj).intValue() : 0);
        recipe.setDifficulty(document.getString("difficulty"));
        recipe.setAuthorName(document.getString("authorName"));

        Object likeCountObj = document.get("likeCount");
        recipe.setLikeCount(likeCountObj instanceof Number ? ((Number) likeCountObj).intValue() : 0);

        recipe.setIngredients(toStringList(document.get("ingredients")));
        recipe.setSteps(toStringList(document.get("steps")));

        return recipe;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
        }
        return result;
    }

    private void displayRecipe(Recipe recipe) {
        if (recipe == null) return;
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
                stepsBuilder.append(i + 1).append(". ").append(steps.get(i)).append("\n\n");
            }
        } else {
            stepsBuilder.append("Đang cập nhật cách chế biến...");
        }
        textSteps.setText(stepsBuilder.toString().trim());

        loadRecipeImage(recipe);
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
