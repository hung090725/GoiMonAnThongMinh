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
        private final View layoutSuggestionInfo;
        private final TextView textRecommendationScore;
        private final TextView textConfidenceLevel;
        private final TextView textCookabilityLevel;
        private final TextView textMatchedIngredients;
        private final TextView textMissingIngredients;
        private final TextView textRecommendationReason;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            imageRecipe = itemView.findViewById(R.id.imageRecipe);
            textRecipeName = itemView.findViewById(R.id.textRecipeName);
            textRecipeDescription = itemView.findViewById(R.id.textRecipeDescription);
            textCalories = itemView.findViewById(R.id.textCalories);
            textCost = itemView.findViewById(R.id.textCost);
            textCookingTime = itemView.findViewById(R.id.textCookingTime);
            textDifficulty = itemView.findViewById(R.id.textDifficulty);
            layoutSuggestionInfo = itemView.findViewById(R.id.layoutSuggestionInfo);
            textRecommendationScore = itemView.findViewById(R.id.textRecommendationScore);
            textConfidenceLevel = itemView.findViewById(R.id.textConfidenceLevel);
            textCookabilityLevel = itemView.findViewById(R.id.textCookabilityLevel);
            textMatchedIngredients = itemView.findViewById(R.id.textMatchedIngredients);
            textMissingIngredients = itemView.findViewById(R.id.textMissingIngredients);
            textRecommendationReason = itemView.findViewById(R.id.textRecommendationReason);
        }

        void bind(Recipe recipe) {
            textRecipeName.setText(getDisplayName(recipe));
            textRecipeDescription.setText(getDescription(recipe));
            textCalories.setText(formatCalories(recipe.getCalories()));
            textCost.setText(formatCost(recipe.getEstimatedCost()));
            textCookingTime.setText(formatCookingTime(recipe.getCookingTime()));
            textDifficulty.setText(formatDifficulty(recipe.getDifficulty()));
            bindSuggestionInfo(recipe);

            loadRecipeImage(recipe);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecipeClick(recipe);
                }
            });
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
                    int resId = itemView.getContext().getResources()
                            .getIdentifier(resName, "drawable", itemView.getContext().getPackageName());
                    imageSource = resId != 0 ? resId : R.drawable.ic_meal_placeholder;
                } else {
                    imageSource = imageUrl;
                }
            }

            Glide.with(itemView.getContext())
                    .load(imageSource)
                    .placeholder(R.drawable.ic_meal_placeholder)
                    .error(R.drawable.ic_meal_placeholder)
                    .centerCrop()
                    .into(imageRecipe);
        }

        private void bindSuggestionInfo(Recipe recipe) {
            boolean hasScore = recipe.getRecommendationScore() != null
                    && recipe.getRecommendationScore() > 0;
            boolean hasMatchPercent = recipe.getMatchPercent() != null
                    && recipe.getMatchPercent() > 0;
            boolean hasMatchedIngredients = recipe.getMatchedIngredients() != null
                    && !recipe.getMatchedIngredients().isEmpty();
            boolean hasMissingIngredients = recipe.getMissingIngredients() != null
                    && !recipe.getMissingIngredients().isEmpty();
            boolean hasReason = !TextUtils.isEmpty(recipe.getRecommendationReason());
            boolean hasConfidence = !TextUtils.isEmpty(recipe.getConfidenceLevel());
            boolean hasCookability = !TextUtils.isEmpty(recipe.getCookabilityLevel());

            if (!hasScore && !hasMatchPercent && !hasMatchedIngredients
                    && !hasMissingIngredients && !hasReason && !hasConfidence && !hasCookability) {
                layoutSuggestionInfo.setVisibility(View.GONE);
                return;
            }

            layoutSuggestionInfo.setVisibility(View.VISIBLE);

            if (hasScore || hasMatchPercent) {
                StringBuilder scoreText = new StringBuilder();
                if (hasScore) {
                    scoreText.append("Điểm phù hợp: ")
                            .append(Math.round(recipe.getRecommendationScore()))
                            .append("/100");
                }
                if (hasMatchPercent) {
                    if (scoreText.length() > 0) {
                        scoreText.append(" • ");
                    }
                    scoreText.append("Trùng ")
                            .append(recipe.getMatchPercent())
                            .append("% nguyên liệu");
                }
                textRecommendationScore.setText(scoreText.toString());
                textRecommendationScore.setVisibility(View.VISIBLE);
            } else {
                textRecommendationScore.setVisibility(View.GONE);
            }

            setTextOrHide(textConfidenceLevel, hasConfidence
                    ? "Mức phù hợp: " + recipe.getConfidenceLevel() : "");
            setTextOrHide(textCookabilityLevel, hasCookability
                    ? "Trạng thái: " + recipe.getCookabilityLevel() : "");
            setListTextOrHide(textMatchedIngredients, "Bạn có: ",
                    recipe.getMatchedIngredients(), 4);
            setListTextOrHide(textMissingIngredients, "Cần bổ sung: ",
                    recipe.getMissingIngredients(), 4);
            setTextOrHide(textRecommendationReason, recipe.getRecommendationReason());
        }
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

    private void setTextOrHide(TextView textView, String value) {
        if (TextUtils.isEmpty(value)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(value);
            textView.setVisibility(View.VISIBLE);
        }
    }

    private void setListTextOrHide(TextView textView, String prefix, List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            textView.setVisibility(View.GONE);
            return;
        }

        textView.setText(prefix + joinLimited(values, limit));
        textView.setVisibility(View.VISIBLE);
    }

    private String joinLimited(List<String> values, int limit) {
        int displayCount = Math.min(values.size(), limit);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < displayCount; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        if (values.size() > displayCount) {
            builder.append(" và ").append(values.size() - displayCount).append(" nguyên liệu khác");
        }
        return builder.toString();
    }
}
