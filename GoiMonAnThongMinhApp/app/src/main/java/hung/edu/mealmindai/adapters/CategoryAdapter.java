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

import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Category;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_admin, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(categories.get(position));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void submitList(List<Category> newCategories) {
        categories.clear();
        if (newCategories != null) {
            categories.addAll(newCategories);
        }
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName, textDescription, textStatus;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textCategoryName);
            textDescription = itemView.findViewById(R.id.textCategoryDescription);
            textStatus = itemView.findViewById(R.id.textCategoryStatus);
        }

        void bind(Category category) {
            textName.setText(TextUtils.isEmpty(category.getName()) ? "Danh mục" : category.getName());
            textDescription.setText(TextUtils.isEmpty(category.getDescription())
                    ? "Chưa có mô tả" : category.getDescription());
            boolean active = !"hidden".equalsIgnoreCase(category.getStatus());
            textStatus.setText(active ? "active" : "hidden");
            textStatus.setTextColor(Color.parseColor(active ? "#2E7D32" : "#4B5563"));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor(active ? "#E8F5E9" : "#F3F4F6"));
            bg.setCornerRadius(28);
            textStatus.setBackground(bg);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
        }
    }
}
