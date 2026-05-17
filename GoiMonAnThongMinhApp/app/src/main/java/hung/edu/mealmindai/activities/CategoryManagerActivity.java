package hung.edu.mealmindai.activities;

import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.adapters.CategoryAdapter;
import hung.edu.mealmindai.models.Category;
import hung.edu.mealmindai.repositories.AdminRecipeRepository;
import hung.edu.mealmindai.repositories.CategoryRepository;

public class CategoryManagerActivity extends AppCompatActivity {
    private AdminRecipeRepository adminRepository;
    private CategoryRepository categoryRepository;
    private CategoryAdapter adapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textState;
    private MaterialButton buttonAdd;
    private boolean actionInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);

        adminRepository = new AdminRecipeRepository();
        categoryRepository = new CategoryRepository();
        initViews();
        checkAdminAndLoad();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerCategories);
        progressBar = findViewById(R.id.progressCategories);
        textState = findViewById(R.id.textCategoryState);
        buttonAdd = findViewById(R.id.buttonAddCategory);
        adapter = new CategoryAdapter(this::showCategoryActionsDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        buttonAdd.setOnClickListener(v -> showCategoryEditDialog(null));
    }

    private void checkAdminAndLoad() {
        adminRepository.checkCurrentUserIsAdmin(new AdminRecipeRepository.AdminCheckCallback() {
            @Override
            public void onResult(boolean isAdmin) {
                if (!isAdmin) {
                    Toast.makeText(CategoryManagerActivity.this,
                            "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                        goToLogin();
                    } else {
                        finish();
                    }
                    return;
                }
                loadCategories();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(CategoryManagerActivity.this,
                        "Lỗi kiểm tra quyền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void loadCategories() {
        setLoading(true);
        categoryRepository.loadCategories(new CategoryRepository.CategoryCallback() {
            @Override
            public void onSuccess(List<Category> categories) {
                setLoading(false);
                List<Category> safeCategories = categories != null ? categories : new ArrayList<>();
                adapter.submitList(safeCategories);
                recyclerView.setVisibility(safeCategories.isEmpty() ? View.GONE : View.VISIBLE);
                textState.setVisibility(safeCategories.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                recyclerView.setVisibility(View.GONE);
                textState.setText("Không tải được dữ liệu. Vui lòng thử lại.");
                textState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showCategoryActionsDialog(Category category) {
        boolean hidden = "hidden".equalsIgnoreCase(category.getStatus());
        String[] actions = hidden
                ? new String[]{"Sửa danh mục", "Khôi phục danh mục"}
                : new String[]{"Sửa danh mục", "Ẩn danh mục"};
        new AlertDialog.Builder(this)
                .setTitle(category.getName())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showCategoryEditDialog(category);
                    } else if (hidden) {
                        restoreCategory(category);
                    } else {
                        hideCategory(category);
                    }
                })
                .show();
    }

    private void showCategoryEditDialog(Category category) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, 0, padding, 0);

        TextInputEditText editName = addInput(layout, "Tên danh mục", category != null ? category.getName() : "");
        TextInputEditText editDesc = addInput(layout, "Mô tả", category != null ? category.getDescription() : "");
        TextInputEditText editIcon = addInput(layout, "Link icon/ảnh", category != null ? category.getIconUrl() : "");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(category == null ? "Thêm danh mục" : "Sửa danh mục")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = editName.getText() != null ? editName.getText().toString().trim() : "";
            String desc = editDesc.getText() != null ? editDesc.getText().toString().trim() : "";
            String icon = editIcon.getText() != null ? editIcon.getText().toString().trim() : "";
            if (TextUtils.isEmpty(name)) {
                editName.setError("Vui lòng nhập tên danh mục");
                return;
            }
            dialog.dismiss();
            if (category == null) {
                addCategory(name, desc, icon);
            } else {
                updateCategory(category.getCategoryId(), name, desc, icon);
            }
        }));
        dialog.show();
    }

    private TextInputEditText addInput(LinearLayout layout, String hint, String value) {
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint(hint);
        TextInputEditText input = new TextInputEditText(this);
        input.setText(value);
        inputLayout.addView(input);
        layout.addView(inputLayout);
        return input;
    }

    private void addCategory(String name, String desc, String icon) {
        if (actionInProgress) {
            return;
        }
        setActionLoading(true);
        categoryRepository.addCategory(name, desc, icon, actionCallback("Đã thêm danh mục"));
    }

    private void updateCategory(String id, String name, String desc, String icon) {
        if (actionInProgress) {
            return;
        }
        setActionLoading(true);
        categoryRepository.updateCategory(id, name, desc, icon, actionCallback("Đã cập nhật danh mục"));
    }

    private void hideCategory(Category category) {
        if (actionInProgress) {
            return;
        }
        setActionLoading(true);
        categoryRepository.hideCategory(category.getCategoryId(), actionCallback("Đã ẩn danh mục"));
    }

    private void restoreCategory(Category category) {
        if (actionInProgress) {
            return;
        }
        setActionLoading(true);
        categoryRepository.restoreCategory(category.getCategoryId(), actionCallback("Đã khôi phục danh mục"));
    }

    private CategoryRepository.ActionCallback actionCallback(String message) {
        return new CategoryRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setActionLoading(false);
                Toast.makeText(CategoryManagerActivity.this, message, Toast.LENGTH_SHORT).show();
                loadCategories();
            }

            @Override
            public void onError(Exception e) {
                setActionLoading(false);
                Toast.makeText(CategoryManagerActivity.this,
                        "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            textState.setVisibility(View.GONE);
        }
    }

    private void setActionLoading(boolean isLoading) {
        actionInProgress = isLoading;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonAdd.setEnabled(!isLoading);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
