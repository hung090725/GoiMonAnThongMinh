package hung.edu.mealmindai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.adapters.MySubmittedRecipeAdapter;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.RecipeRepository;

public class MySubmittedRecipesActivity extends AppCompatActivity {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private MySubmittedRecipeAdapter adapter;
    private RecyclerView recyclerView;
    private View stateLayout;
    private TextView textState;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_submitted_recipes);

        initViews();
        setupRecyclerView();
        loadSubmittedRecipes();
    }

    private void initViews() {
        ImageButton buttonBack = findViewById(R.id.buttonMyRecipesBack);
        recyclerView = findViewById(R.id.recyclerMySubmittedRecipes);
        stateLayout = findViewById(R.id.layoutMyRecipesState);
        textState = findViewById(R.id.textMyRecipesState);
        progressBar = findViewById(R.id.progressMyRecipes);
        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new MySubmittedRecipeAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadSubmittedRecipes() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            goToLogin();
            return;
        }

        setLoading(true);
        db.collection("recipes")
                .whereEqualTo("authorId", currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    setLoading(false);
                    List<Recipe> recipes = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        try {
                            recipes.add(RecipeRepository.mapRecipeDocument(document));
                        } catch (Exception ignored) {
                            // Bỏ qua document lỗi để màn hình vẫn hiển thị các món hợp lệ.
                        }
                    }

                    Collections.sort(recipes, (first, second) -> {
                        long firstTime = first.getCreatedAt() != null
                                ? first.getCreatedAt().toDate().getTime() : 0L;
                        long secondTime = second.getCreatedAt() != null
                                ? second.getCreatedAt().toDate().getTime() : 0L;
                        return Long.compare(secondTime, firstTime);
                    });

                    if (recipes.isEmpty()) {
                        showState("Bạn chưa gửi công thức nào");
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        stateLayout.setVisibility(View.GONE);
                        adapter.submitList(recipes);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showState("Không tải được công thức của tôi");
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            stateLayout.setVisibility(View.GONE);
        }
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
