package hung.edu.mealmindai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.adapters.AiSuggestionHistoryAdapter;
import hung.edu.mealmindai.models.AiSuggestionHistory;
import hung.edu.mealmindai.repositories.AiSuggestionHistoryRepository;

public class AiSuggestionHistoryActivity extends AppCompatActivity {
    private AiSuggestionHistoryRepository repository;
    private AiSuggestionHistoryAdapter adapter;
    private RecyclerView recyclerView;
    private View stateLayout;
    private TextView textState;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_suggestion_history);

        repository = new AiSuggestionHistoryRepository();
        initViews();
        setupRecyclerView();
        loadHistory();
    }

    private void initViews() {
        ImageButton buttonBack = findViewById(R.id.buttonHistoryBack);
        recyclerView = findViewById(R.id.recyclerAiHistory);
        stateLayout = findViewById(R.id.layoutAiHistoryState);
        textState = findViewById(R.id.textAiHistoryState);
        progressBar = findViewById(R.id.progressAiHistory);
        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new AiSuggestionHistoryAdapter(new AiSuggestionHistoryAdapter.OnHistoryClickListener() {
            @Override
            public void onHistoryClick(AiSuggestionHistory history) {
                openFirstSuggestedRecipe(history);
            }

            @Override
            public void onHistoryDelete(AiSuggestionHistory history) {
                confirmDelete(history);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadHistory() {
        setLoading(true);
        repository.loadCurrentUserHistory(new AiSuggestionHistoryRepository.HistoryCallback() {
            @Override
            public void onSuccess(List<AiSuggestionHistory> histories) {
                setLoading(false);
                if (histories == null || histories.isEmpty()) {
                    showState("Chưa có lịch sử gợi ý");
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    stateLayout.setVisibility(View.GONE);
                    adapter.submitList(histories);
                }
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                showState("Không tải được lịch sử. Vui lòng thử lại.");
                Toast.makeText(AiSuggestionHistoryActivity.this,
                        "Lỗi lịch sử: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoginRequired() {
                setLoading(false);
                Toast.makeText(AiSuggestionHistoryActivity.this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void openFirstSuggestedRecipe(AiSuggestionHistory history) {
        if (history.getSuggestedRecipeIds() == null || history.getSuggestedRecipeIds().isEmpty()
                || TextUtils.isEmpty(history.getSuggestedRecipeIds().get(0))) {
            Toast.makeText(this, "Lịch sử này chưa có món để mở", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, history.getSuggestedRecipeIds().get(0));
        startActivity(intent);
    }

    private void confirmDelete(AiSuggestionHistory history) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa lịch sử")
                .setMessage("Bạn muốn xóa lượt gợi ý này?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> deleteHistory(history))
                .show();
    }

    private void deleteHistory(AiSuggestionHistory history) {
        repository.deleteHistory(history.getHistoryId(), new AiSuggestionHistoryRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AiSuggestionHistoryActivity.this, "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
                loadHistory();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AiSuggestionHistoryActivity.this,
                        "Lỗi xóa lịch sử: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
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
