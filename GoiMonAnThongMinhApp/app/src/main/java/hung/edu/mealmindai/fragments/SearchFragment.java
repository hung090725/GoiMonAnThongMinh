package hung.edu.mealmindai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.activities.RecipeDetailActivity;
import hung.edu.mealmindai.adapters.RecipeAdapter;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.models.User;
import hung.edu.mealmindai.repositories.SearchHistoryRepository;
import hung.edu.mealmindai.repositories.UserRepository;
import hung.edu.mealmindai.utils.RecommendationEngine;

public class SearchFragment extends Fragment {

    private EditText editIngredientsInput;
    private Button buttonSearch;
    private ProgressBar progressSearch;
    private RecyclerView recyclerResults;
    private TextView textResultsTitle;
    private LinearLayout layoutStates;
    private TextView textSearchState;
    
    private RecipeAdapter recipeAdapter;
    private FirebaseFirestore db;
    private SearchHistoryRepository historyRepository;
    private UserRepository userRepository;

    // User preferences for recommendation
    private String healthGoal = "";
    private double userBudget = 0;
    private int availableTime = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        db = FirebaseFirestore.getInstance();
        historyRepository = new SearchHistoryRepository();
        
        setupRecyclerView();
        fetchUserProfile();

        buttonSearch.setOnClickListener(v -> performSearch());
    }

    private void initViews(View view) {
        editIngredientsInput = view.findViewById(R.id.editIngredientsInput);
        buttonSearch = view.findViewById(R.id.buttonSearchRecipes);
        progressSearch = view.findViewById(R.id.progressSearch);
        recyclerResults = view.findViewById(R.id.recyclerSearchResults);
        textResultsTitle = view.findViewById(R.id.textResultsTitle);
        layoutStates = view.findViewById(R.id.layoutSearchStates);
        textSearchState = view.findViewById(R.id.textSearchState);
    }

    private void setupRecyclerView() {
        recipeAdapter = new RecipeAdapter(new ArrayList<>(), recipe -> {
            Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getRecipeId());
            startActivity(intent);
        });
        recyclerResults.setAdapter(recipeAdapter);
    }

    private void fetchUserProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        if (userRepository == null) userRepository = new UserRepository();

        userRepository.getCurrentUserProfile(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    healthGoal = user.getHealthGoal() != null ? user.getHealthGoal() : "";
                    userBudget = user.getMealBudget() != null ? user.getMealBudget() : 0.0;
                    availableTime = user.getAvailableTime() != null ? user.getAvailableTime() : 0;
                }
            }

            @Override
            public void onError(Exception e) {
                // Giữ giá trị mặc định nếu lỗi
            }
        });
    }

    private void performSearch() {
        String input = editIngredientsInput.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            Toast.makeText(requireContext(), "Vui lòng nhập nguyên liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tách nguyên liệu theo dấu phẩy và xóa khoảng trắng thừa
        String[] splitInputs = input.split(",");
        List<String> userIngredients = new ArrayList<>();
        for (String s : splitInputs) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) userIngredients.add(trimmed);
        }
        
        setLoading(true);
        db.collection("recipes")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Sử dụng Map để lọc trùng lặp theo TÊN món ăn (viết thường)
                    java.util.Map<String, RecommendationEngine.RecipeWithScore> uniqueRecipes = new java.util.HashMap<>();
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Recipe recipe = mapDocumentToRecipe(doc);
                        String normalizedName = recipe.getName() != null ? recipe.getName().toLowerCase().trim() : "";
                        if (normalizedName.isEmpty()) continue;
                        
                        // ... tính toán điểm ...
                        double ingredientScore = RecommendationEngine.calculateIngredientScore(recipe, userIngredients);
                        
                        if (ingredientScore > 0) {
                            // ... các thành phần điểm khác ...
                            // (giữ nguyên logic tính điểm)
                            double healthScore = RecommendationEngine.calculateHealthScore(recipe, healthGoal);
                            double budgetScore = RecommendationEngine.calculateBudgetScore(recipe, userBudget);
                            double timeScore = RecommendationEngine.calculateTimeScore(recipe, availableTime);
                            double totalScore = (ingredientScore * 0.40) + (healthScore * 0.20) + (budgetScore * 0.15) + (timeScore * 0.15) + (70.0 * 0.10);

                            // Nếu đã có món tên này, chỉ giữ lại bản có điểm cao hơn
                            if (!uniqueRecipes.containsKey(normalizedName) || totalScore > uniqueRecipes.get(normalizedName).score) {
                                uniqueRecipes.put(normalizedName, new RecommendationEngine.RecipeWithScore(recipe, totalScore));
                            }
                        }
                    }

                    // Chuyển Map sang List để sắp xếp
                    List<RecommendationEngine.RecipeWithScore> scoredRecipes = new ArrayList<>(uniqueRecipes.values());
                    RecommendationEngine.sortRecipesByScore(scoredRecipes);

                    // Hiển thị kết quả
                    List<Recipe> finalRecipes = new ArrayList<>();
                    for (RecommendationEngine.RecipeWithScore rws : scoredRecipes) {
                        finalRecipes.add(rws.recipe);
                    }

                    setLoading(false);
                    displayResults(finalRecipes);
                    
                    // Lưu lịch sử
                    historyRepository.saveSearchHistory(input, userIngredients);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showErrorState("Lỗi kết nối: " + e.getMessage());
                });
    }

    private void setLoading(boolean isLoading) {
        progressSearch.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerResults.setVisibility(View.GONE);
        layoutStates.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        textResultsTitle.setVisibility(View.GONE);
        buttonSearch.setEnabled(!isLoading);
    }

    private void displayResults(List<Recipe> recipes) {
        if (recipes.isEmpty()) {
            showNoResultState();
        } else {
            recipeAdapter.submitList(recipes);
            recyclerResults.setVisibility(View.VISIBLE);
            layoutStates.setVisibility(View.GONE);
            textResultsTitle.setVisibility(View.VISIBLE);
        }
    }

    private void showNoResultState() {
        textSearchState.setText(getString(R.string.search_no_result));
        layoutStates.setVisibility(View.VISIBLE);
        recyclerResults.setVisibility(View.GONE);
        textResultsTitle.setVisibility(View.GONE);
    }

    private void showErrorState(String error) {
        textSearchState.setText(getString(R.string.search_error) + ": " + error);
        layoutStates.setVisibility(View.VISIBLE);
        recyclerResults.setVisibility(View.GONE);
        textResultsTitle.setVisibility(View.GONE);
    }

    private Recipe mapDocumentToRecipe(DocumentSnapshot document) {
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
        
        recipe.setIngredients(toStringList(document.get("ingredients")));
        recipe.setSteps(toStringList(document.get("steps")));
        
        return recipe;
    }

    private List<String> toStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item != null) result.add(item.toString());
            }
        }
        return result;
    }
}
