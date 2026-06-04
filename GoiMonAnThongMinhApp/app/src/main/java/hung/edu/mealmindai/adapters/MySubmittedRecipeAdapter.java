package hung.edu.mealmindai.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;

public class MySubmittedRecipeAdapter extends RecyclerView.Adapter<MySubmittedRecipeAdapter.ViewHolder> {
    private final List<Recipe> recipes = new ArrayList<>();
    private final DecimalFormat costFormat = new DecimalFormat("#,###");

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_submitted_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textStatus;
        private final TextView textDescription;
        private final TextView textMeta;
        private final TextView textRejectReason;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textSubmittedRecipeName);
            textStatus = itemView.findViewById(R.id.textSubmittedRecipeStatus);
            textDescription = itemView.findViewById(R.id.textSubmittedRecipeDescription);
            textMeta = itemView.findViewById(R.id.textSubmittedRecipeMeta);
            textRejectReason = itemView.findViewById(R.id.textSubmittedRejectReason);
        }

        void bind(Recipe recipe) {
            textName.setText(TextUtils.isEmpty(recipe.getName()) ? "Công thức món ăn" : recipe.getName());
            textDescription.setText(TextUtils.isEmpty(recipe.getDescription())
                    ? "Chưa có mô tả" : recipe.getDescription());
            bindStatus(recipe.getStatus());

            double cost = recipe.getEstimatedCost() != null ? recipe.getEstimatedCost() : 0;
            int time = recipe.getCookingTime() != null ? recipe.getCookingTime() : 0;
            textMeta.setText(costFormat.format(cost).replace(",", ".") + "đ • " + time + " phút");

            if ("rejected".equals(recipe.getStatus()) && !TextUtils.isEmpty(recipe.getRejectReason())) {
                textRejectReason.setText("Lý do từ chối: " + recipe.getRejectReason());
                textRejectReason.setVisibility(View.VISIBLE);
            } else {
                textRejectReason.setVisibility(View.GONE);
            }
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
            GradientDrawable background = new GradientDrawable();
            background.setColor(bgColor);
            background.setCornerRadius(28);
            textStatus.setBackground(background);
            textStatus.setTextColor(textColor);
            textStatus.setText(label);
        }
    }
}
