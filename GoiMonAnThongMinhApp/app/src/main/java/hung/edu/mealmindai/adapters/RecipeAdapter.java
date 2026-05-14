package hung.edu.mealmindai.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private final List<Recipe> recipes;
    private final OnRecipeClickListener listener;
    private final DecimalFormat costFormat = new DecimalFormat("#,###");

    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes != null ? recipes : new ArrayList<>();
        this.listener = listener;
    }

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void submitList(List<Recipe> newRecipes) {
        recipes.clear();
        if (newRecipes != null) {
            recipes.addAll(newRecipes);
        }
        notifyDataSetChanged();
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageRecipe;
        private final TextView textRecipeName;
        private final TextView textRecipeDescription;
        private final TextView textCalories;
        private final TextView textCost;
        private final TextView textCookingTime;
        private final TextView textDifficulty;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            imageRecipe = itemView.findViewById(R.id.imageRecipe);
            textRecipeName = itemView.findViewById(R.id.textRecipeName);
            textRecipeDescription = itemView.findViewById(R.id.textRecipeDescription);
            textCalories = itemView.findViewById(R.id.textCalories);
            textCost = itemView.findViewById(R.id.textCost);
            textCookingTime = itemView.findViewById(R.id.textCookingTime);
            textDifficulty = itemView.findViewById(R.id.textDifficulty);
        }

        void bind(Recipe recipe) {
            textRecipeName.setText(getDisplayName(recipe));
            textRecipeDescription.setText(getDescription(recipe));
            textCalories.setText(formatCalories(recipe.getCalories()));
            textCost.setText(formatCost(recipe.getEstimatedCost()));
            textCookingTime.setText(formatCookingTime(recipe.getCookingTime()));
            textDifficulty.setText(formatDifficulty(recipe.getDifficulty()));

            String imageUrl = recipe.getImageUrl();
            if (!TextUtils.isEmpty(imageUrl)) {
                if (imageUrl.startsWith("res://")) {
                    String resName = imageUrl.replace("res://", "");
                    int resId = itemView.getContext().getResources().getIdentifier(resName, "drawable", itemView.getContext().getPackageName());
                    Glide.with(itemView.getContext())
                            .load(resId != 0 ? resId : R.drawable.ic_meal_placeholder)
                            .placeholder(R.drawable.ic_meal_placeholder)
                            .error(R.drawable.ic_meal_placeholder)
                            .centerCrop()
                            .into(imageRecipe);
                } else {
                    Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_meal_placeholder)
                            .error(R.drawable.ic_meal_placeholder)
                            .centerCrop()
                            .into(imageRecipe);
                }
            } else {
                imageRecipe.setImageResource(R.drawable.ic_meal_placeholder);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecipeClick(recipe);
                }
            });
        }
    }

    private String getDisplayName(Recipe recipe) {
        String name = recipe.getName();
        return TextUtils.isEmpty(name) ? "Món ăn" : name;
    }

    private String getDescription(Recipe recipe) {
        String description = recipe.getDescription();
        return TextUtils.isEmpty(description)
                ? "Món ăn phù hợp cho thực đơn hằng ngày."
                : description;
    }

    private String formatCalories(Integer calories) {
        int value = calories != null ? calories : 0;
        return value + " kcal";
    }

    private String formatCost(Double estimatedCost) {
        double value = estimatedCost != null ? estimatedCost : 0;
        return costFormat.format(value).replace(",", ".") + "đ";
    }

    private String formatCookingTime(Integer cookingTime) {
        int value = cookingTime != null ? cookingTime : 0;
        return value + " phút";
    }

    private String formatDifficulty(String difficulty) {
        return TextUtils.isEmpty(difficulty) ? "Dễ" : difficulty;
    }
}
