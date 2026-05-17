package hung.edu.mealmindai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.adapters.AdminRecipeAdapter;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.AdminRecipeRepository;

public class AdminRecipeActivity extends AppCompatActivity {
    private AdminRecipeRepository repository;
    private AdminRecipeAdapter adapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textState;
    private EditText editSearch;
    private String currentStatus = "all";
    private String currentKeyword = "";
    private boolean adminAllowed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_recipe);

        repository = new AdminRecipeRepository();
        initViews();
        checkAdminAndLoad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null && adminAllowed) {
            loadRecipes();
        }
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        editSearch = findViewById(R.id.editAdminRecipeSearch);
        recyclerView = findViewById(R.id.recyclerAdminRecipes);
        progressBar = findViewById(R.id.progressAdminRecipes);
        textState = findViewById(R.id.textAdminRecipeState);
        MaterialButton buttonRefresh = findViewById(R.id.buttonRefreshAdminRecipes);

        adapter = new AdminRecipeAdapter(recipeId -> {
            Intent intent = new Intent(this, AdminRecipeDetailActivity.class);
            intent.putExtra(AdminRecipeDetailActivity.EXTRA_RECIPE_ID, recipeId);
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupFilter(R.id.filterAll, "all");
        setupFilter(R.id.filterApproved, "approved");
        setupFilter(R.id.filterPending, "pending");
        setupFilter(R.id.filterRejected, "rejected");
        setupFilter(R.id.filterHidden, "hidden");
        buttonRefresh.setOnClickListener(v -> loadRecipes());

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentKeyword = s != null ? s.toString().trim() : "";
                loadRecipes();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFilter(int buttonId, String status) {
        MaterialButton button = findViewById(buttonId);
        button.setOnClickListener(v -> {
            currentStatus = status;
            loadRecipes();
        });
    }

    private void checkAdminAndLoad() {
        repository.checkCurrentUserIsAdmin(new AdminRecipeRepository.AdminCheckCallback() {
            @Override
            public void onResult(boolean isAdmin) {
                if (!isAdmin) {
                    Toast.makeText(AdminRecipeActivity.this,
                            "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                        goToLogin();
                    } else {
                        finish();
                    }
                    return;
                }
                adminAllowed = true;
                loadRecipes();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AdminRecipeActivity.this,
                        "Lỗi kiểm tra quyền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void loadRecipes() {
        setLoading(true);
        repository.searchRecipes(currentKeyword, currentStatus, new AdminRecipeRepository.RecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                setLoading(false);
                List<Recipe> safeRecipes = recipes != null ? recipes : new ArrayList<>();
                adapter.submitList(safeRecipes);
                recyclerView.setVisibility(safeRecipes.isEmpty() ? View.GONE : View.VISIBLE);
                textState.setVisibility(safeRecipes.isEmpty() ? View.VISIBLE : View.GONE);
                textState.setText(safeRecipes.isEmpty() ? getEmptyMessageForStatus() : "");
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                recyclerView.setVisibility(View.GONE);
                textState.setVisibility(View.VISIBLE);
                textState.setText("Không tải được dữ liệu. Vui lòng thử lại.");
            }
        });
    }

    private String getEmptyMessageForStatus() {
        if ("approved".equalsIgnoreCase(currentStatus)) {
            return "Chưa có món đã duyệt";
        } else if ("pending".equalsIgnoreCase(currentStatus)) {
            return "Không có món nào chờ duyệt";
        } else if ("rejected".equalsIgnoreCase(currentStatus)) {
            return "Chưa có món bị từ chối";
        } else if ("hidden".equalsIgnoreCase(currentStatus)) {
            return "Chưa có món bị ẩn";
        }
        return "Không có món phù hợp";
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            textState.setVisibility(View.GONE);
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
