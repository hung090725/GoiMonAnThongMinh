package hung.edu.mealmindai.adapters;

import android.text.TextUtils;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;

public class PendingRecipeAdapter extends RecyclerView.Adapter<PendingRecipeAdapter.PendingRecipeViewHolder> {
    private final List<Recipe> recipes = new ArrayList<>();
    private final OnPendingRecipeClickListener listener;
    private final DecimalFormat costFormat = new DecimalFormat("#,###");

    public interface OnPendingRecipeClickListener {
        void onPendingRecipeClick(String recipeId);
    }

    public PendingRecipeAdapter(OnPendingRecipeClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PendingRecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_recipe, parent, false);
        return new PendingRecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingRecipeViewHolder holder, int position) {
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

    class PendingRecipeViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageRecipe;
        private final TextView textName;
        private final TextView textDescription;
        private final TextView textAuthor;
        private final TextView textCost;
        private final TextView textTime;
        private final TextView textDifficulty;

        PendingRecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            imageRecipe = itemView.findViewById(R.id.imagePendingRecipe);
            textName = itemView.findViewById(R.id.textPendingRecipeName);
            textDescription = itemView.findViewById(R.id.textPendingRecipeDescription);
            textAuthor = itemView.findViewById(R.id.textPendingRecipeAuthor);
            textCost = itemView.findViewById(R.id.textPendingRecipeCost);
            textTime = itemView.findViewById(R.id.textPendingRecipeTime);
            textDifficulty = itemView.findViewById(R.id.textPendingRecipeDifficulty);
        }

        void bind(Recipe recipe) {
            textName.setText(TextUtils.isEmpty(recipe.getName()) ? "Món chưa đặt tên" : recipe.getName());
            textDescription.setText(TextUtils.isEmpty(recipe.getDescription())
                    ? "Chưa có mô tả" : recipe.getDescription());
            textAuthor.setText("Tác giả: " + (TextUtils.isEmpty(recipe.getAuthorName())
                    ? "Người dùng MealMind" : recipe.getAuthorName()));
            double cost = recipe.getEstimatedCost() != null ? recipe.getEstimatedCost() : 0;
            textCost.setText(costFormat.format(cost).replace(",", ".") + "đ");
            int time = recipe.getCookingTime() != null ? recipe.getCookingTime() : 0;
            textTime.setText(time + " phút");
            textDifficulty.setText(TextUtils.isEmpty(recipe.getDifficulty()) ? "Dễ" : recipe.getDifficulty());

            String imageUrl = recipe.getImageUrl();
            if (TextUtils.isEmpty(imageUrl)) {
                imageRecipe.setVisibility(View.GONE);
            } else {
                imageRecipe.setVisibility(View.VISIBLE);
            }
            Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target,
                                                    boolean isFirstResource) {
                            imageRecipe.setVisibility(View.GONE);
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                                                       DataSource dataSource, boolean isFirstResource) {
                            imageRecipe.setVisibility(View.VISIBLE);
                            return false;
                        }
                    })
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imageRecipe);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPendingRecipeClick(recipe.getRecipeId());
                }
            });
        }
    }
}
