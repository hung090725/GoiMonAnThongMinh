package hung.edu.mealmindai.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.models.User;
import hung.edu.mealmindai.repositories.RecipeRepository;
import hung.edu.mealmindai.repositories.UserRepository;

public class AddRecipeActivity extends AppCompatActivity {

    private TextInputEditText editName, editDescription, editImageUrl;
    private TextInputEditText editCalories, editCost, editTime, editDifficulty, editCategory;
    private TextInputEditText editIngredients, editSteps;
    private MaterialButton buttonSubmit;
    private MaterialCardView cardImagePreview;
    private ImageView imageRecipePreview;
    private ProgressBar progressBar;
    private final Handler previewHandler = new Handler(Looper.getMainLooper());
    private Runnable previewRunnable;

    private RecipeRepository recipeRepository;
    private UserRepository userRepository;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        recipeRepository = new RecipeRepository();
        userRepository = new UserRepository();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupToolbar();
        setupImagePreview();

        buttonSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void initViews() {
        editName = findViewById(R.id.editRecipeName);
        editDescription = findViewById(R.id.editRecipeDescription);
        editImageUrl = findViewById(R.id.editRecipeImageUrl);
        editCalories = findViewById(R.id.editRecipeCalories);
        editCost = findViewById(R.id.editRecipeCost);
        editTime = findViewById(R.id.editRecipeTime);
        editDifficulty = findViewById(R.id.editRecipeDifficulty);
        editCategory = findViewById(R.id.editRecipeCategory);
        editIngredients = findViewById(R.id.editRecipeIngredients);
        editSteps = findViewById(R.id.editRecipeSteps);
        cardImagePreview = findViewById(R.id.cardImagePreview);
        imageRecipePreview = findViewById(R.id.imageRecipePreview);
        buttonSubmit = findViewById(R.id.buttonSubmitRecipe);
        progressBar = findViewById(R.id.progressAddRecipe);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupImagePreview() {
        editImageUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (previewRunnable != null) {
                    previewHandler.removeCallbacks(previewRunnable);
                }

                previewRunnable = () -> updateImagePreview(s != null ? s.toString().trim() : "");
                previewHandler.postDelayed(previewRunnable, 450);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateImagePreview(String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            cardImagePreview.setVisibility(View.GONE);
            imageRecipePreview.setImageResource(R.drawable.ic_meal_placeholder);
            return;
        }

        if (isBase64ImageDataUrl(imageUrl)) {
            editImageUrl.setError("Ảnh base64 quá dài, hãy dùng link ảnh http/https");
            cardImagePreview.setVisibility(View.GONE);
            imageRecipePreview.setImageResource(R.drawable.ic_meal_placeholder);
            return;
        }

        editImageUrl.setError(null);
        cardImagePreview.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_meal_placeholder)
                .error(R.drawable.ic_meal_placeholder)
                .centerCrop()
                .into(imageRecipePreview);
    }

    private boolean isBase64ImageDataUrl(String value) {
        return value != null && value.trim().toLowerCase().startsWith("data:image");
    }

    private void validateAndSubmit() {
        String name = editName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String imageUrl = editImageUrl.getText().toString().trim();
        String caloriesStr = editCalories.getText().toString().trim();
        String costStr = editCost.getText().toString().trim();
        String timeStr = editTime.getText().toString().trim();
        String difficulty = editDifficulty.getText().toString().trim();
        String categoryId = editCategory.getText().toString().trim();
        String ingredientsStr = editIngredients.getText().toString().trim();
        String stepsStr = editSteps.getText().toString().trim();

        if (isBase64ImageDataUrl(imageUrl)) {
            editImageUrl.setError("Không dùng ảnh base64, hãy dán link ảnh http/https");
            return;
        }
        if (TextUtils.isEmpty(name)) {
            editName.setError("Vui lòng nhập tên món");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            editDescription.setError("Vui lòng nhập mô tả");
            return;
        }
        if (TextUtils.isEmpty(difficulty)) {
            editDifficulty.setError("Vui lòng nhập độ khó");
            return;
        }
        if (TextUtils.isEmpty(categoryId)) {
            editCategory.setError("Vui lòng nhập danh mục");
            return;
        }
        if (TextUtils.isEmpty(ingredientsStr)) {
            editIngredients.setError("Vui lòng nhập nguyên liệu");
            return;
        }
        if (TextUtils.isEmpty(stepsStr)) {
            editSteps.setError("Vui lòng nhập cách làm");
            return;
        }

        Integer calories = parsePositiveInteger(caloriesStr, editCalories, "Calo phải là số hợp lệ");
        if (calories == null) return;

        Double cost = parsePositiveDouble(costStr, editCost, "Chi phí phải là số hợp lệ");
        if (cost == null) return;

        Integer time = parsePositiveInteger(timeStr, editTime, "Thời gian phải là số hợp lệ");
        if (time == null) return;

        if (time <= 0) {
            editTime.setError("Thời gian nấu phải lớn hơn 0");
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Lấy tên tác giả từ profile
        userRepository.getCurrentUserProfile(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User userProfile) {
                String authorName = (userProfile != null && !TextUtils.isEmpty(userProfile.getFullName()))
                        ? userProfile.getFullName()
                        : (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail());

                submitRecipe(name, description, imageUrl, calories, cost, time, difficulty, categoryId, ingredientsStr, stepsStr, currentUser.getUid(), authorName);
            }

            @Override
            public void onError(Exception e) {
                // Nếu lỗi lấy profile, dùng email làm authorName
                submitRecipe(name, description, imageUrl, calories, cost, time, difficulty, categoryId, ingredientsStr, stepsStr, currentUser.getUid(), currentUser.getEmail());
            }
        });
    }

    private void submitRecipe(String name, String description, String imageUrl, int calories, double cost, int time, 
                              String difficulty, String categoryId, String ingredientsStr, String stepsStr,
                              String authorId, String authorName) {
        
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setTitle(name);
        recipe.setDescription(description);
        recipe.setImageUrl(imageUrl);
        recipe.setCalories(calories);
        recipe.setEstimatedCost(cost);
        recipe.setCookingTime(time);
        recipe.setDifficulty(difficulty);
        
        // Chuyển đổi chuỗi nguyên liệu sang List
        List<String> ingredientsList = new ArrayList<>();
        String[] ingArray = ingredientsStr.split(",");
        for (String s : ingArray) {
            if (!s.trim().isEmpty()) ingredientsList.add(s.trim());
        }
        recipe.setIngredients(ingredientsList);

        // Chuyển đổi các bước thực hiện sang List
        List<String> stepsList = new ArrayList<>();
        String[] stepArray = stepsStr.split("\n");
        for (String s : stepArray) {
            if (!s.trim().isEmpty()) stepsList.add(s.trim());
        }
        recipe.setSteps(stepsList);

        recipe.setAuthorId(authorId);
        recipe.setAuthorName(authorName);
        recipe.setStatus("pending");
        recipe.setLikeCount(0);
        recipe.setCategoryId(categoryId);

        recipeRepository.submitRecipeForReview(recipe, new RecipeRepository.RecipeActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(AddRecipeActivity.this, "Đã gửi công thức, vui lòng chờ admin duyệt", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(AddRecipeActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Integer parsePositiveInteger(String value, TextInputEditText input, String errorMessage) {
        if (TextUtils.isEmpty(value)) {
            return 0;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                input.setError("Giá trị không được âm");
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            input.setError(errorMessage);
            return null;
        }
    }

    private Double parsePositiveDouble(String value, TextInputEditText input, String errorMessage) {
        if (TextUtils.isEmpty(value)) {
            return 0.0;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 0) {
                input.setError("Giá trị không được âm");
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            input.setError(errorMessage);
            return null;
        }
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonSubmit.setEnabled(!isLoading);
    }
}
