package hung.edu.mealmindai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.repositories.AdminRecipeRepository;

public class AdminDashboardActivity extends AppCompatActivity {
    private TextView textAdminName, textStatPending, textStatApproved, textStatRejected, textStatUsers, textStatHidden;
    private ProgressBar progressAdminStats;
    private GridLayout gridAdminStats;
    private AdminRecipeRepository repository;
    private boolean adminAllowed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        repository = new AdminRecipeRepository();
        initViews();
        checkAdminAndLoad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null && adminAllowed) {
            reloadDashboardStats();
        }
    }

    private void initViews() {
        textAdminName = findViewById(R.id.textAdminName);
        textStatPending = findViewById(R.id.textStatPending);
        textStatApproved = findViewById(R.id.textStatApproved);
        textStatRejected = findViewById(R.id.textStatRejected);
        textStatUsers = findViewById(R.id.textStatUsers);
        textStatHidden = findViewById(R.id.textStatHidden);
        progressAdminStats = findViewById(R.id.progressAdminStats);
        gridAdminStats = findViewById(R.id.gridAdminStats);
        MaterialButton buttonPending = findViewById(R.id.buttonPendingRecipes);
        MaterialButton buttonAdd = findViewById(R.id.buttonAdminAddRecipe);
        MaterialButton buttonManageRecipes = findViewById(R.id.buttonManageRecipes);
        MaterialButton buttonManageCategories = findViewById(R.id.buttonManageCategories);
        MaterialButton buttonStatistics = findViewById(R.id.buttonStatistics);
        MaterialButton buttonLogout = findViewById(R.id.buttonAdminLogout);

        buttonPending.setOnClickListener(v -> startActivity(new Intent(this, PendingRecipeActivity.class)));
        buttonAdd.setOnClickListener(v -> startActivity(new Intent(this, AddRecipeActivity.class)));
        buttonManageRecipes.setOnClickListener(v -> startActivity(new Intent(this, AdminRecipeActivity.class)));
        buttonManageCategories.setOnClickListener(v -> startActivity(new Intent(this, CategoryManagerActivity.class)));
        buttonStatistics.setOnClickListener(v -> startActivity(new Intent(this, StatisticsActivity.class)));
        buttonLogout.setOnClickListener(v -> logout());

        setStatsZero();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            textAdminName.setText(user.getEmail() != null ? user.getEmail() : "Admin");
        }
    }

    private void checkAdminAndLoad() {
        repository.checkCurrentUserIsAdmin(new AdminRecipeRepository.AdminCheckCallback() {
            @Override
            public void onResult(boolean isAdmin) {
                if (!isAdmin) {
                    Toast.makeText(AdminDashboardActivity.this,
                            "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                        goToLogin();
                    } else {
                        finish();
                    }
                    return;
                }
                adminAllowed = true;
                reloadDashboardStats();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Lỗi kiểm tra quyền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void reloadDashboardStats() {
        setStatsLoading(true);
        repository.loadDashboardStats(new AdminRecipeRepository.StatsCallback() {
            @Override
            public void onSuccess(AdminRecipeRepository.DashboardStats stats) {
                setStatsLoading(false);
                textStatPending.setText("Chờ duyệt\n" + stats.pendingRecipes);
                textStatApproved.setText("Đã duyệt\n" + stats.approvedRecipes);
                textStatRejected.setText("Từ chối\n" + stats.rejectedRecipes);
                textStatUsers.setText("Người dùng\n" + stats.users);
                textStatHidden.setText("Đã ẩn\n" + stats.hiddenRecipes);
            }

            @Override
            public void onError(Exception e) {
                setStatsLoading(false);
                setStatsZero();
                Toast.makeText(AdminDashboardActivity.this,
                        "Không tải được dữ liệu. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setStatsLoading(boolean isLoading) {
        progressAdminStats.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        gridAdminStats.setAlpha(isLoading ? 0.55f : 1f);
    }

    private void setStatsZero() {
        textStatPending.setText("Chờ duyệt\n0");
        textStatApproved.setText("Đã duyệt\n0");
        textStatRejected.setText("Từ chối\n0");
        textStatUsers.setText("Người dùng\n0");
        textStatHidden.setText("Đã ẩn\n0");
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
