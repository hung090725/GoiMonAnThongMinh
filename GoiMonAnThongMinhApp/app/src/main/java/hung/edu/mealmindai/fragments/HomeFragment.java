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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.activities.SmartMealChatActivity;
import hung.edu.mealmindai.activities.RecipeDetailActivity;
import hung.edu.mealmindai.adapters.RecipeAdapter;
import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.NotificationRepository;
import hung.edu.mealmindai.repositories.RecipeRepository;
import hung.edu.mealmindai.utils.RecentRecipeStore;
import hung.edu.mealmindai.utils.SeedDataHelper;

public class HomeFragment extends Fragment {

    private ProgressBar progressRecipes;
    private NestedScrollView homeScrollView;
    private TextView emptyRecipesText;
    private TextView errorRecipesText;
    private TextView buttonHomeScrollTop;
    private TextView buttonHomeScrollBottom;
    private TextView textRecentRecipeTitle;
    private MaterialButton buttonOpenSmartChat;
    private RecyclerView recipesRecyclerView, recentRecipesRecyclerView;
    private RecipeAdapter recipeAdapter, recentRecipeAdapter;
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
        createDailySuggestionNotification();
        // Seed dữ liệu mẫu nếu collection recipes đang rỗng, rồi tải danh sách món.
        SeedDataHelper.seedRecipesIfNeeded(this::loadApprovedRecipes);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (recipeRepository != null) {
            loadRecentRecipes();
        }
    }

    private void initViews(View view) {
        progressRecipes = view.findViewById(R.id.progressRecipes);
        homeScrollView = view.findViewById(R.id.homeScrollView);
        emptyRecipesText = view.findViewById(R.id.textEmptyRecipes);
        errorRecipesText = view.findViewById(R.id.textErrorRecipes);
        buttonHomeScrollTop = view.findViewById(R.id.buttonHomeScrollTop);
        buttonHomeScrollBottom = view.findViewById(R.id.buttonHomeScrollBottom);
        textRecentRecipeTitle = view.findViewById(R.id.textRecentRecipeTitle);
        buttonOpenSmartChat = view.findViewById(R.id.buttonOpenSmartChat);
        recentRecipesRecyclerView = view.findViewById(R.id.recyclerRecentRecipes);
        recipesRecyclerView = view.findViewById(R.id.recyclerRecipes);
        recipeRepository = new RecipeRepository();
        buttonOpenSmartChat.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SmartMealChatActivity.class)));
    }

    private void setupRecyclerView() {
        recipeAdapter = new RecipeAdapter(new ArrayList<>(), this::openRecipeDetail);
        recentRecipeAdapter = new RecipeAdapter(new ArrayList<>(), this::openRecipeDetail);
        recentRecipesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentRecipesRecyclerView.setAdapter(recentRecipeAdapter);
        recipesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recipesRecyclerView.setAdapter(recipeAdapter);
        buttonHomeScrollTop.setOnClickListener(v -> homeScrollView.smoothScrollTo(0, 0));
        buttonHomeScrollBottom.setOnClickListener(v ->
                homeScrollView.post(() -> homeScrollView.smoothScrollTo(0, homeScrollView.getChildAt(0).getBottom())));
        updateScrollActionVisibility();
    }

    private void loadRecentRecipes() {
        List<String> recentIds = RecentRecipeStore.getRecentRecipeIds(requireContext());
        if (recentIds.isEmpty()) {
            hideRecentRecipes();
            return;
        }

        recipeRepository.loadRecipesByIds(recentIds, new RecipeRepository.RecipeCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                if (!isAdded()) {
                    return;
                }
                if (recipes == null || recipes.isEmpty()) {
                    hideRecentRecipes();
                    return;
                }
                recentRecipeAdapter.submitList(recipes);
                textRecentRecipeTitle.setVisibility(View.VISIBLE);
                recentRecipesRecyclerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Exception exception) {
                if (!isAdded()) {
                    return;
                }
                hideRecentRecipes();
            }
        });
    }

    private void hideRecentRecipes() {
        if (textRecentRecipeTitle != null) {
            textRecentRecipeTitle.setVisibility(View.GONE);
        }
        if (recentRecipesRecyclerView != null) {
            recentRecipesRecyclerView.setVisibility(View.GONE);
        }
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
        recipesRecyclerView.post(this::updateScrollActionVisibility);
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

    private void createDailySuggestionNotification() {
        new NotificationRepository().createDailySuggestionNotificationIfNeeded(
                new NotificationRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        // Silent success: notification will appear in the notification screen.
                    }

                    @Override
                    public void onError(Exception e) {
                        // Notification is optional; Home should keep loading normally.
                    }
                });
    }

    private void updateScrollActionVisibility() {
        if (buttonHomeScrollTop == null || buttonHomeScrollBottom == null || recipesRecyclerView == null) {
            return;
        }

        boolean hasManyRecipes = recipeAdapter != null && recipeAdapter.getItemCount() > 1;
        buttonHomeScrollTop.setVisibility(hasManyRecipes ? View.VISIBLE : View.GONE);
        buttonHomeScrollBottom.setVisibility(hasManyRecipes ? View.VISIBLE : View.GONE);
    }
}
