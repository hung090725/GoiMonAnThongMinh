package hung.edu.mealmindai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.activities.RecipeDetailActivity;
import hung.edu.mealmindai.adapters.RecipeAdapter;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.FavoriteRepository;

/**
 * Hiển thị danh sách món ăn yêu thích của user hiện tại.
 * Dùng lại RecipeAdapter để giữ giao diện đồng nhất với HomeFragment.
 */
public class FavoriteFragment extends Fragment {

    private ProgressBar progressFavorites;
    private RecyclerView recyclerFavorites;
    private LinearLayout layoutEmptyFavorites;
    private TextView textErrorFavorites;
    private RecipeAdapter recipeAdapter;
    private FavoriteRepository favoriteRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        favoriteRepository = new FavoriteRepository();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại danh sách mỗi khi quay về tab Yêu thích,
        // vì user có thể vừa thêm/bỏ yêu thích ở RecipeDetailActivity.
        loadFavoriteRecipes();
    }

    private void initViews(View view) {
        progressFavorites = view.findViewById(R.id.progressFavorites);
        recyclerFavorites = view.findViewById(R.id.recyclerFavorites);
        layoutEmptyFavorites = view.findViewById(R.id.layoutEmptyFavorites);
        textErrorFavorites = view.findViewById(R.id.textErrorFavorites);
    }

    private void setupRecyclerView() {
        recipeAdapter = new RecipeAdapter(new ArrayList<>(), this::openRecipeDetail);
        recyclerFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerFavorites.setAdapter(recipeAdapter);
    }

    private void loadFavoriteRecipes() {
        setLoading(true);
        favoriteRepository.loadFavoriteRecipes(new FavoriteRepository.FavoriteRecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                if (!isAdded()) return;
                setLoading(false);
                if (recipes == null || recipes.isEmpty()) {
                    showEmptyState();
                } else {
                    showRecipes(recipes);
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                setLoading(false);
                showErrorState();
                Toast.makeText(requireContext(),
                        getString(R.string.favorite_error) + ": " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressFavorites.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerFavorites.setVisibility(View.GONE);
        layoutEmptyFavorites.setVisibility(View.GONE);
        textErrorFavorites.setVisibility(View.GONE);
    }

    private void showRecipes(List<Recipe> recipes) {
        recipeAdapter.submitList(recipes);
        recyclerFavorites.setVisibility(View.VISIBLE);
        layoutEmptyFavorites.setVisibility(View.GONE);
        textErrorFavorites.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recyclerFavorites.setVisibility(View.GONE);
        layoutEmptyFavorites.setVisibility(View.VISIBLE);
        textErrorFavorites.setVisibility(View.GONE);
    }

    private void showErrorState() {
        recyclerFavorites.setVisibility(View.GONE);
        layoutEmptyFavorites.setVisibility(View.GONE);
        textErrorFavorites.setVisibility(View.VISIBLE);
    }

    private void openRecipeDetail(Recipe recipe) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getRecipeId());
        startActivity(intent);
    }
}
