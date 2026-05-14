package hung.edu.mealmindai.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "recipeId";

    private ImageView imageRecipe;
    private TextView textName, textDescription, textCalories, textTime, textCost, textIngredients, textSteps;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupToolbar();

        String recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (recipeId != null) {
            loadRecipeDetails(recipeId);
        } else {
            Toast.makeText(this, R.string.error_load_recipe, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        imageRecipe = findViewById(R.id.imageRecipeDetail);
        textName = findViewById(R.id.textRecipeName);
        textDescription = findViewById(R.id.textRecipeDescription);
        textCalories = findViewById(R.id.textDetailCalories);
        textTime = findViewById(R.id.textDetailTime);
        textCost = findViewById(R.id.textDetailCost);
        textIngredients = findViewById(R.id.textIngredients);
        textSteps = findViewById(R.id.textSteps);
        progressBar = findViewById(R.id.progressDetail);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadRecipeDetails(String recipeId) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("recipes").document(recipeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        Recipe recipe = mapDocumentToRecipe(documentSnapshot);
                        displayRecipe(recipe);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.error_load_recipe, Toast.LENGTH_SHORT).show();
                });
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
        
        recipe.setIngredients((java.util.List<String>) document.get("ingredients"));
        recipe.setSteps((java.util.List<String>) document.get("steps"));
        
        return recipe;
    }

    private void displayRecipe(Recipe recipe) {
        if (recipe == null) return;

        textName.setText(recipe.getName() != null ? recipe.getName() : "Không tên");
        textDescription.setText(recipe.getDescription() != null ? recipe.getDescription() : "");
        
        // Stats - Đảm bảo không bị lỗi nếu số liệu bằng null
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

        // Image
        String imageUrl = recipe.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageUrl.startsWith("res://")) {
                String resName = imageUrl.replace("res://", "");
                int resId = getResources().getIdentifier(resName, "drawable", getPackageName());
                Glide.with(this)
                        .load(resId != 0 ? resId : R.drawable.ic_meal_placeholder)
                        .placeholder(R.drawable.ic_meal_placeholder)
                        .error(R.drawable.ic_meal_placeholder)
                        .centerCrop()
                        .into(imageRecipe);
            } else {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_meal_placeholder)
                        .error(R.drawable.ic_meal_placeholder)
                        .centerCrop()
                        .into(imageRecipe);
            }
        } else {
            imageRecipe.setImageResource(R.drawable.ic_meal_placeholder);
        }
    }
}
