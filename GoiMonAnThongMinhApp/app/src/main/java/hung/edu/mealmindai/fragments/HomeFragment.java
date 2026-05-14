package hung.edu.mealmindai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.activities.RecipeDetailActivity;
import hung.edu.mealmindai.adapters.RecipeAdapter;
import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.RecipeRepository;
import hung.edu.mealmindai.utils.SeedDataHelper;

public class HomeFragment extends Fragment {

    private ProgressBar progressRecipes;
    private TextView emptyRecipesText;
    private TextView errorRecipesText;
    private RecyclerView recipesRecyclerView;
    private RecipeAdapter recipeAdapter;
    private RecipeRepository recipeRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        // Seed dữ liệu mẫu nếu collection recipes đang rỗng (chỉ chạy 1 lần)
        SeedDataHelper.seedRecipesIfNeeded();
        loadApprovedRecipes();
    }

    private void initViews(View view) {
        progressRecipes = view.findViewById(R.id.progressRecipes);
        emptyRecipesText = view.findViewById(R.id.textEmptyRecipes);
        errorRecipesText = view.findViewById(R.id.textErrorRecipes);
        recipesRecyclerView = view.findViewById(R.id.recyclerRecipes);
        recipeRepository = new RecipeRepository();
    }

    private void setupRecyclerView() {
        recipeAdapter = new RecipeAdapter(new ArrayList<>(), this::openRecipeDetail);
        recipesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recipesRecyclerView.setAdapter(recipeAdapter);
    }

    private void loadApprovedRecipes() {
        setLoading(true);
        recipeRepository.loadApprovedRecipes(new RecipeRepository.RecipeCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                if (!isAdded()) {
                    return;
                }

                setLoading(false);
                if (recipes == null || recipes.isEmpty()) {
                    showEmptyState();
                } else {
                    showRecipes(recipes);
                }
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded()) {
                    return;
                }

                setLoading(false);
                showErrorState();
                Toast.makeText(requireContext(),
                        "Lỗi tải món ăn: " + exception.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressRecipes.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recipesRecyclerView.setVisibility(View.GONE);
        emptyRecipesText.setVisibility(View.GONE);
        errorRecipesText.setVisibility(View.GONE);
    }

    private void showRecipes(List<Recipe> recipes) {
        recipeAdapter.submitList(recipes);
        recipesRecyclerView.setVisibility(View.VISIBLE);
        emptyRecipesText.setVisibility(View.GONE);
        errorRecipesText.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recipesRecyclerView.setVisibility(View.GONE);
        emptyRecipesText.setVisibility(View.VISIBLE);
        errorRecipesText.setVisibility(View.GONE);
    }

    private void showErrorState() {
        recipesRecyclerView.setVisibility(View.GONE);
        emptyRecipesText.setVisibility(View.GONE);
        errorRecipesText.setVisibility(View.VISIBLE);
    }

    private void openRecipeDetail(Recipe recipe) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getRecipeId());
        startActivity(intent);
    }
}
