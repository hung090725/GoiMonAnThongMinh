package hung.edu.mealmindai.activities;

import android.os.Bundle;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.repositories.PantryRepository;

public class PantryActivity extends AppCompatActivity {
    private static final List<String> SUGGESTED_INGREDIENTS = Arrays.asList(
            "trứng", "cà chua", "hành", "tỏi", "thịt heo", "thịt gà",
            "cá", "đậu hũ", "rau", "nấm", "bún", "gạo"
    );

    private View pantryRoot;
    private EditText editPantryIngredients;
    private TextView textPantryState, textPantryCount;
    private ChipGroup chipGroupSavedIngredients, chipGroupPantrySuggestions;
    private ProgressBar progressPantry;
    private MaterialButton buttonSavePantry;
    private PantryRepository pantryRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry);

        pantryRepository = new PantryRepository();
        pantryRoot = findViewById(R.id.pantryRoot);
        ImageButton buttonBack = findViewById(R.id.buttonPantryBack);
        editPantryIngredients = findViewById(R.id.editPantryIngredients);
        textPantryState = findViewById(R.id.textPantryState);
        textPantryCount = findViewById(R.id.textPantryCount);
        chipGroupSavedIngredients = findViewById(R.id.chipGroupSavedIngredients);
        chipGroupPantrySuggestions = findViewById(R.id.chipGroupPantrySuggestions);
        progressPantry = findViewById(R.id.progressPantry);
        buttonSavePantry = findViewById(R.id.buttonSavePantry);

        buttonBack.setOnClickListener(v -> finish());
        buttonSavePantry.setOnClickListener(v -> savePantry());
        renderSuggestionChips();
        loadPantry();
    }

    private void loadPantry() {
        setLoading(true);
        pantryRepository.loadCurrentUserPantry(new PantryRepository.PantryCallback() {
            @Override
            public void onSuccess(List<String> ingredients) {
                setLoading(false);
                if (ingredients == null || ingredients.isEmpty()) {
                    textPantryState.setText("Bạn chưa lưu nguyên liệu nào. Hãy nhập nguyên liệu đang có trong bếp.");
                    textPantryState.setVisibility(View.VISIBLE);
                    renderSavedIngredients(new ArrayList<>());
                    return;
                }

                editPantryIngredients.setText(joinIngredients(ingredients));
                textPantryState.setText("Đã tải " + ingredients.size() + " nguyên liệu từ tủ lạnh của bạn.");
                textPantryState.setVisibility(View.VISIBLE);
                renderSavedIngredients(ingredients);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                textPantryState.setText("Không thể tải tủ lạnh: " + e.getMessage());
                textPantryState.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoginRequired() {
                setLoading(false);
                textPantryState.setText("Vui lòng đăng nhập để dùng Tủ lạnh của tôi.");
                textPantryState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void savePantry() {
        String rawText = editPantryIngredients.getText() != null
                ? editPantryIngredients.getText().toString().trim() : "";
        List<String> ingredients = parseIngredients(rawText);
        if (ingredients.isEmpty()) {
            showMessage("Vui lòng nhập ít nhất một nguyên liệu");
            return;
        }

        setLoading(true);
        pantryRepository.saveCurrentUserPantry(ingredients, new PantryRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                textPantryState.setText("Đã lưu " + ingredients.size() + " nguyên liệu. Bạn có thể dùng ở màn Tìm món.");
                textPantryState.setVisibility(View.VISIBLE);
                renderSavedIngredients(ingredients);
                showMessage("Đã lưu Tủ lạnh của tôi");
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                showMessage("Lỗi lưu tủ lạnh: " + e.getMessage());
            }

            @Override
            public void onLoginRequired() {
                setLoading(false);
                showMessage("Vui lòng đăng nhập để lưu nguyên liệu");
            }
        });
    }

    private void renderSavedIngredients(List<String> ingredients) {
        chipGroupSavedIngredients.removeAllViews();
        int count = ingredients != null ? ingredients.size() : 0;
        textPantryCount.setText(count + " nguyên liệu đã lưu");

        if (ingredients == null || ingredients.isEmpty()) {
            Chip emptyChip = createChip("Chưa có nguyên liệu", false);
            emptyChip.setEnabled(false);
            chipGroupSavedIngredients.addView(emptyChip);
            return;
        }

        for (String ingredient : ingredients) {
            Chip chip = createChip(ingredient, true);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                List<String> currentIngredients = parseIngredients(editPantryIngredients.getText() != null
                        ? editPantryIngredients.getText().toString() : "");
                removeIgnoreCase(currentIngredients, ingredient);
                editPantryIngredients.setText(joinIngredients(currentIngredients));
                renderSavedIngredients(currentIngredients);
            });
            chipGroupSavedIngredients.addView(chip);
        }
    }

    private void renderSuggestionChips() {
        chipGroupPantrySuggestions.removeAllViews();
        for (String ingredient : SUGGESTED_INGREDIENTS) {
            Chip chip = createChip("+ " + ingredient, false);
            chip.setOnClickListener(v -> addIngredientToInput(ingredient));
            chipGroupPantrySuggestions.addView(chip);
        }
    }

    private Chip createChip(String text, boolean saved) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setTextSize(13);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setTextColor(ContextCompat.getColor(this,
                saved ? R.color.primary_green : R.color.accent_orange));
        chip.setChipBackgroundColorResource(saved ? R.color.primary_light : R.color.accent_orange_soft);
        chip.setChipStrokeColorResource(saved ? R.color.primary_green : R.color.accent_orange);
        chip.setChipStrokeWidth(1);
        chip.setMinHeight(dp(34));
        chip.setEnsureMinTouchTargetSize(false);
        return chip;
    }

    private void addIngredientToInput(String ingredient) {
        List<String> ingredients = parseIngredients(editPantryIngredients.getText() != null
                ? editPantryIngredients.getText().toString() : "");
        if (!containsIgnoreCase(ingredients, ingredient)) {
            ingredients.add(ingredient);
        }
        editPantryIngredients.setText(joinIngredients(ingredients));
        editPantryIngredients.setSelection(editPantryIngredients.getText().length());
        renderSavedIngredients(ingredients);
    }

    private void removeIgnoreCase(List<String> values, String candidate) {
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i).equalsIgnoreCase(candidate)) {
                values.remove(i);
            }
        }
    }

    private List<String> parseIngredients(String rawText) {
        List<String> ingredients = new ArrayList<>();
        if (TextUtils.isEmpty(rawText)) {
            return ingredients;
        }

        String[] tokens = rawText.split("[,;\\n]+");
        for (String token : tokens) {
            String ingredient = token.trim();
            if (!ingredient.isEmpty() && !containsIgnoreCase(ingredients, ingredient)) {
                ingredients.add(ingredient);
            }
        }
        return ingredients;
    }

    private boolean containsIgnoreCase(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String joinIngredients(List<String> ingredients) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ingredients.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(ingredients.get(i));
        }
        return builder.toString();
    }

    private void setLoading(boolean loading) {
        progressPantry.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonSavePantry.setEnabled(!loading);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showMessage(String message) {
        Snackbar.make(pantryRoot, message, Snackbar.LENGTH_SHORT)
                .setAnchorView(buttonSavePantry)
                .show();
    }
}
