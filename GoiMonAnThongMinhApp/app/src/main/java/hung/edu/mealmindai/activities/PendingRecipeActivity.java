package hung.edu.mealmindai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import hung.edu.mealmindai.adapters.PendingRecipeAdapter;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.AdminRecipeRepository;

public class PendingRecipeActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView textEmpty;
    private TextView textError;
    private RecyclerView recyclerView;
    private PendingRecipeAdapter adapter;
    private AdminRecipeRepository repository;
    private boolean adminAllowed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_recipe);

        repository = new AdminRecipeRepository();
        initViews();
        checkAdminAndLoad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null && adminAllowed) {
            loadPendingRecipes();
        }
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressPendingRecipes);
        textEmpty = findViewById(R.id.textPendingEmpty);
        textError = findViewById(R.id.textPendingError);
        recyclerView = findViewById(R.id.recyclerPendingRecipes);
        MaterialButton buttonRefresh = findViewById(R.id.buttonRefreshPending);

        adapter = new PendingRecipeAdapter(recipeId -> {
            Intent intent = new Intent(this, PendingRecipeDetailActivity.class);
            intent.putExtra(PendingRecipeDetailActivity.EXTRA_RECIPE_ID, recipeId);
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        buttonRefresh.setOnClickListener(v -> loadPendingRecipes());
    }

    private void checkAdminAndLoad() {
        repository.checkCurrentUserIsAdmin(new AdminRecipeRepository.AdminCheckCallback() {
            @Override
            public void onResult(boolean isAdmin) {
                if (!isAdmin) {
                    Toast.makeText(PendingRecipeActivity.this,
                            "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                        goToLogin();
                    } else {
                        finish();
                    }
                    return;
                }
                adminAllowed = true;
                loadPendingRecipes();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(PendingRecipeActivity.this,
                        "Lỗi kiểm tra quyền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void loadPendingRecipes() {
        setLoading(true);
        repository.loadPendingRecipes(new AdminRecipeRepository.RecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                setLoading(false);
                List<Recipe> safeRecipes = recipes != null ? recipes : new ArrayList<>();
                adapter.submitList(safeRecipes);
                recyclerView.setVisibility(safeRecipes.isEmpty() ? View.GONE : View.VISIBLE);
                textEmpty.setVisibility(safeRecipes.isEmpty() ? View.VISIBLE : View.GONE);
                textError.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                recyclerView.setVisibility(View.GONE);
                textEmpty.setVisibility(View.GONE);
                textError.setVisibility(View.VISIBLE);
                textError.setText("Không tải được dữ liệu. Vui lòng thử lại.");
                Toast.makeText(PendingRecipeActivity.this,
                        "Lỗi tải món chờ duyệt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            textEmpty.setVisibility(View.GONE);
            textError.setVisibility(View.GONE);
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
