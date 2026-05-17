package hung.edu.mealmindai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.adapters.NotificationAdapter;
import hung.edu.mealmindai.models.AppNotification;
import hung.edu.mealmindai.repositories.NotificationRepository;

public class NotificationActivity extends AppCompatActivity {
    private NotificationRepository repository;
    private NotificationAdapter adapter;
    private RecyclerView recyclerView;
    private View stateLayout;
    private TextView textState;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        repository = new NotificationRepository();
        initViews();
        setupRecyclerView();
        loadNotifications();
    }

    private void initViews() {
        ImageButton buttonBack = findViewById(R.id.buttonNotificationBack);
        MaterialButton buttonRefresh = findViewById(R.id.buttonRefreshNotifications);
        recyclerView = findViewById(R.id.recyclerNotifications);
        stateLayout = findViewById(R.id.layoutNotificationState);
        textState = findViewById(R.id.textNotificationState);
        progressBar = findViewById(R.id.progressNotifications);

        buttonBack.setOnClickListener(v -> finish());
        buttonRefresh.setOnClickListener(v -> loadNotifications());
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this::handleNotificationClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        setLoading(true);
        repository.loadCurrentUserNotifications(new NotificationRepository.NotificationCallback() {
            @Override
            public void onSuccess(List<AppNotification> notifications) {
                setLoading(false);
                if (notifications == null || notifications.isEmpty()) {
                    showState("Chưa có thông báo");
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    stateLayout.setVisibility(View.GONE);
                    adapter.submitList(notifications);
                }
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                showState("Không tải được thông báo. Vui lòng thử lại.");
                Toast.makeText(NotificationActivity.this,
                        "Lỗi thông báo: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoginRequired() {
                setLoading(false);
                Toast.makeText(NotificationActivity.this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void handleNotificationClick(AppNotification notification) {
        repository.markAsRead(notification.getNotificationId());
        if (!TextUtils.isEmpty(notification.getRecipeId())) {
            Intent intent = new Intent(this, RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, notification.getRecipeId());
            startActivity(intent);
        } else {
            Toast.makeText(this, notification.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : recyclerView.getVisibility());
        stateLayout.setVisibility(isLoading ? View.GONE : stateLayout.getVisibility());
    }

    private void showState(String message) {
        textState.setText(message);
        recyclerView.setVisibility(View.GONE);
        stateLayout.setVisibility(View.VISIBLE);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
