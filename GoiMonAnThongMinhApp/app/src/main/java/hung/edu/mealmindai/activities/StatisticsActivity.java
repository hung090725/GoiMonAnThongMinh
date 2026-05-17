package hung.edu.mealmindai.activities;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.repositories.AdminRecipeRepository;

public class StatisticsActivity extends AppCompatActivity {
    private AdminRecipeRepository repository;
    private ProgressBar progressBar;
    private TextView textError;
    private TextView textTotalUsers, textTotalRecipes, textApproved, textPending;
    private TextView textRejected, textHidden, textFavorites, textSearchHistory;
    private View statsContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        repository = new AdminRecipeRepository();
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

        progressBar = findViewById(R.id.progressStatistics);
        textError = findViewById(R.id.textStatisticsError);
        statsContent = findViewById(R.id.layoutStatisticsContent);
        textTotalUsers = findViewById(R.id.textStatTotalUsers);
        textTotalRecipes = findViewById(R.id.textStatTotalRecipes);
        textApproved = findViewById(R.id.textStatApprovedRecipes);
        textPending = findViewById(R.id.textStatPendingRecipes);
        textRejected = findViewById(R.id.textStatRejectedRecipes);
        textHidden = findViewById(R.id.textStatHiddenRecipes);
        textFavorites = findViewById(R.id.textStatFavorites);
        textSearchHistory = findViewById(R.id.textStatSearchHistory);
        MaterialButton buttonRefresh = findViewById(R.id.buttonRefreshStatistics);
        buttonRefresh.setOnClickListener(v -> loadStats());
    }

    private void checkAdminAndLoad() {
        repository.checkCurrentUserIsAdmin(new AdminRecipeRepository.AdminCheckCallback() {
            @Override
            public void onResult(boolean isAdmin) {
                if (!isAdmin) {
                    Toast.makeText(StatisticsActivity.this,
                            "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                        goToLogin();
                    } else {
                        finish();
                    }
                    return;
                }
                loadStats();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(StatisticsActivity.this,
                        "Lỗi kiểm tra quyền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void loadStats() {
        setLoading(true);
        repository.loadDashboardStats(new AdminRecipeRepository.StatsCallback() {
            @Override
            public void onSuccess(AdminRecipeRepository.DashboardStats stats) {
                setLoading(false);
                bindStats(stats);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                statsContent.setVisibility(View.GONE);
                textError.setVisibility(View.VISIBLE);
                textError.setText("Không tải được dữ liệu. Vui lòng thử lại.");
            }
        });
    }

    private void bindStats(AdminRecipeRepository.DashboardStats stats) {
        statsContent.setVisibility(View.VISIBLE);
        textError.setVisibility(View.GONE);
        textTotalUsers.setText(String.valueOf(stats.users));
        textTotalRecipes.setText(String.valueOf(stats.totalRecipes));
        textApproved.setText(String.valueOf(stats.approvedRecipes));
        textPending.setText(String.valueOf(stats.pendingRecipes));
        textRejected.setText(String.valueOf(stats.rejectedRecipes));
        textHidden.setText(String.valueOf(stats.hiddenRecipes));
        textFavorites.setText(String.valueOf(stats.favorites));
        textSearchHistory.setText(String.valueOf(stats.searchHistory));
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
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
