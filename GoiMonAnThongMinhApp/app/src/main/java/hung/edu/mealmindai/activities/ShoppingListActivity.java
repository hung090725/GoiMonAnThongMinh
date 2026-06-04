package hung.edu.mealmindai.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.RecipeRepository;
import hung.edu.mealmindai.repositories.ShoppingListRepository;

public class ShoppingListActivity extends AppCompatActivity {
    public static final String EXTRA_RECIPE_ID = "recipeId";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final ShoppingListRepository repository = new ShoppingListRepository();
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private final List<String> items = new ArrayList<>();

    private String recipeId;
    private String recipeName = "";
    private TextView textRecipeName;
    private LinearLayout layoutItems;
    private MaterialButton buttonSave;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);

        initViews();
        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (TextUtils.isEmpty(recipeId)) {
            Toast.makeText(this, "Thiếu mã món ăn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadRecipe(recipeId);
    }

    private void initViews() {
        ImageButton buttonBack = findViewById(R.id.buttonShoppingBack);
        textRecipeName = findViewById(R.id.textShoppingRecipeName);
        layoutItems = findViewById(R.id.layoutShoppingItems);
        buttonSave = findViewById(R.id.buttonSaveShoppingList);
        progressBar = findViewById(R.id.progressShoppingList);

        buttonBack.setOnClickListener(v -> finish());
        buttonSave.setOnClickListener(v -> saveShoppingList());
    }

    private void loadRecipe(String recipeId) {
        setLoading(true);
        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Không tìm thấy món ăn", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Recipe recipe = RecipeRepository.mapRecipeDocument(documentSnapshot);
                    recipeName = TextUtils.isEmpty(recipe.getName()) ? "Món ăn" : recipe.getName();
                    textRecipeName.setText(recipeName);
                    items.clear();
                    if (recipe.getIngredients() != null) {
                        for (String ingredient : recipe.getIngredients()) {
                            if (!TextUtils.isEmpty(ingredient)) {
                                items.add(ingredient.trim());
                            }
                        }
                    }
                    renderItems();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi tải nguyên liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void renderItems() {
        layoutItems.removeAllViews();
        checkBoxes.clear();

        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Món này đang cập nhật nguyên liệu.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setTextSize(15f);
            layoutItems.addView(empty);
            return;
        }

        for (String item : items) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(item);
            checkBox.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            checkBox.setTextSize(16f);
            checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary_green)));
            checkBox.setPadding(4, 10, 4, 10);
            layoutItems.addView(checkBox);
            checkBoxes.add(checkBox);
        }
    }

    private void saveShoppingList() {
        List<String> checkedItems = new ArrayList<>();
        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isChecked()) {
                checkedItems.add(checkBox.getText().toString());
            }
        }

        buttonSave.setEnabled(false);
        repository.saveShoppingList(recipeId, recipeName, items, checkedItems,
                new ShoppingListRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        buttonSave.setEnabled(true);
                        Toast.makeText(ShoppingListActivity.this,
                                "Đã lưu danh sách mua", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        buttonSave.setEnabled(true);
                        Toast.makeText(ShoppingListActivity.this,
                                "Lỗi lưu danh sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLoginRequired() {
                        buttonSave.setEnabled(true);
                        Toast.makeText(ShoppingListActivity.this,
                                "Vui lòng đăng nhập để lưu danh sách mua", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonSave.setEnabled(!isLoading);
    }
}
