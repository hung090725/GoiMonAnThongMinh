package hung.edu.mealmindai.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

public class AdminRecipeAdapter extends RecyclerView.Adapter<AdminRecipeAdapter.AdminRecipeViewHolder> {
    private final List<Recipe> recipes = new ArrayList<>();
    private final OnAdminRecipeClickListener listener;
    private final DecimalFormat costFormat = new DecimalFormat("#,###");

    public interface OnAdminRecipeClickListener {
        void onRecipeClick(String recipeId);
    }

    public AdminRecipeAdapter(OnAdminRecipeClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminRecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_recipe, parent, false);
        return new AdminRecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminRecipeViewHolder holder, int position) {
        holder.bind(recipes.get(position));
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

    class AdminRecipeViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageRecipe;
        private final TextView textName, textDescription, textAuthor, textStatus;
        private final TextView textCalories, textCost, textTime, textDifficulty;

        AdminRecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            imageRecipe = itemView.findViewById(R.id.imageAdminRecipe);
            textName = itemView.findViewById(R.id.textAdminRecipeName);
            textDescription = itemView.findViewById(R.id.textAdminRecipeDescription);
            textAuthor = itemView.findViewById(R.id.textAdminRecipeAuthor);
            textStatus = itemView.findViewById(R.id.textAdminRecipeStatus);
            textCalories = itemView.findViewById(R.id.textAdminRecipeCalories);
            textCost = itemView.findViewById(R.id.textAdminRecipeCost);
            textTime = itemView.findViewById(R.id.textAdminRecipeTime);
            textDifficulty = itemView.findViewById(R.id.textAdminRecipeDifficulty);
        }

        void bind(Recipe recipe) {
            textName.setText(TextUtils.isEmpty(recipe.getName()) ? "Món ăn" : recipe.getName());
            textDescription.setText(TextUtils.isEmpty(recipe.getDescription())
                    ? "Chưa có mô tả" : recipe.getDescription());
            textAuthor.setText("Tác giả: " + (TextUtils.isEmpty(recipe.getAuthorName())
                    ? "MealMind" : recipe.getAuthorName()));
            bindStatus(recipe.getStatus());
            textCalories.setText((recipe.getCalories() != null ? recipe.getCalories() : 0) + " kcal");
            double cost = recipe.getEstimatedCost() != null ? recipe.getEstimatedCost() : 0;
            textCost.setText(costFormat.format(cost).replace(",", ".") + "đ");
            textTime.setText((recipe.getCookingTime() != null ? recipe.getCookingTime() : 0) + " phút");
            textDifficulty.setText(TextUtils.isEmpty(recipe.getDifficulty()) ? "Dễ" : recipe.getDifficulty());

            Object imageSource = resolveRecipeImage(recipe);
            imageRecipe.setVisibility(View.VISIBLE);
            Glide.with(itemView.getContext())
                    .load(imageSource)
                    .placeholder(R.drawable.ic_meal_placeholder)
                    .error(R.drawable.ic_meal_placeholder)
                    .centerCrop()
                    .into(imageRecipe);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecipeClick(recipe.getRecipeId());
                }
            });
        }

        private Object resolveRecipeImage(Recipe recipe) {
            int localImage = getLocalImageForRecipe(recipe);
            if (localImage != 0) {
                return localImage;
            }

            String imageUrl = recipe.getImageUrl();
            if (TextUtils.isEmpty(imageUrl)) {
                return R.drawable.ic_meal_placeholder;
            }

            if (imageUrl.startsWith("res://")) {
                String resName = imageUrl.replace("res://", "");
                int resId = itemView.getContext().getResources()
                        .getIdentifier(resName, "drawable", itemView.getContext().getPackageName());
                return resId != 0 ? resId : R.drawable.ic_meal_placeholder;
            }

            return imageUrl;
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

        private void bindStatus(String status) {
            String normalized = status == null ? "pending" : status;
            int textColor;
            int bgColor;
            String label;
            switch (normalized) {
                case "approved":
                    label = "Đã duyệt";
                    textColor = Color.parseColor("#2E7D32");
                    bgColor = Color.parseColor("#E8F5E9");
                    break;
                case "rejected":
                    label = "Bị từ chối";
                    textColor = Color.parseColor("#C62828");
                    bgColor = Color.parseColor("#FFEBEE");
                    break;
                case "hidden":
                    label = "Đã ẩn";
                    textColor = Color.parseColor("#4B5563");
                    bgColor = Color.parseColor("#F3F4F6");
                    break;
                default:
                    label = "Chờ duyệt";
                    textColor = Color.parseColor("#FF9800");
                    bgColor = Color.parseColor("#FFF3E0");
                    break;
            }
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(bgColor);
            bg.setCornerRadius(28);
            textStatus.setBackground(bg);
            textStatus.setTextColor(textColor);
            textStatus.setText(label);
        }
    }
}
