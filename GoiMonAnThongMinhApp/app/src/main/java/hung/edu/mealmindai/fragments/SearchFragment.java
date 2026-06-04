package hung.edu.mealmindai.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.activities.RecipeDetailActivity;
import hung.edu.mealmindai.adapters.RecipeAdapter;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.repositories.RecipeRepository;
import hung.edu.mealmindai.models.User;
import hung.edu.mealmindai.repositories.PantryRepository;
import hung.edu.mealmindai.repositories.SearchHistoryRepository;
import hung.edu.mealmindai.repositories.UserRepository;
import hung.edu.mealmindai.utils.RecommendationEngine;
import hung.edu.mealmindai.utils.SearchKeywordStore;
import hung.edu.mealmindai.utils.VoiceInputHelper;

public class SearchFragment extends Fragment {
    private static final List<String> SEARCH_STOPWORDS = Arrays.asList(
            "ăn", "an", "món", "mon", "món ăn", "mon an", "muốn", "muon",
            "tìm", "tim", "kiếm", "kiem", "gợi ý", "goi y", "cho", "tôi",
            "toi", "mình", "minh", "có", "co", "với", "voi", "và", "va"
    );
    private static final List<String> COMMON_SEARCH_INGREDIENTS = Arrays.asList(
            "cơm", "trứng", "cà chua", "thịt heo", "thịt gà", "ức gà", "rau",
            "hành", "tỏi", "sả", "ớt", "đậu hũ", "đậu phụ", "cá", "bí đỏ",
            "rau ngót", "thịt băm", "nấm", "mì", "bún", "gạo"
    );

    // -------------------------------------------------------------------------
    // Views
    // -------------------------------------------------------------------------
    private android.widget.EditText editIngredientsInput;
    private MaterialButton buttonSearch;
    private MaterialButton buttonUsePantry;
    private ImageButton buttonVoice;
    private TextView textVoiceStatus;
    private TextView textClearRecentSearch;
    private ProgressBar progressSearch;
    private RecyclerView recyclerResults;
    private TextView textResultsTitle;
    private android.widget.LinearLayout layoutStates;
    private LinearLayout layoutRecentSearchContainer, layoutRecentSearchChips;
    private TextView textSearchState;
    private View rootView;

    // -------------------------------------------------------------------------
    // Data & Logic
    // -------------------------------------------------------------------------
    private RecipeAdapter recipeAdapter;
    private FirebaseFirestore db;
    private SearchHistoryRepository historyRepository;
    private UserRepository userRepository;
    private PantryRepository pantryRepository;
    private VoiceInputHelper voiceInputHelper;

    // Thông tin hồ sơ người dùng để cá nhân hóa điểm gợi ý
    private String healthGoal = "";
    private double userBudget = 0;
    private int availableTime = 0;

    // -------------------------------------------------------------------------
    // Permission Launcher (Activity Result API)
    // -------------------------------------------------------------------------
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            startSystemVoiceInput();
                        } else {
                            showMessage(getString(R.string.voice_permission_denied));
                        }
                    }
            );

    private final ActivityResultLauncher<Intent> speechInputLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        setVoiceListeningState(false, getString(R.string.voice_hint));
                        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                            showMessage("Chưa nhận được giọng nói. Hãy thử lại.");
                            return;
                        }

                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches == null || matches.isEmpty()) {
                            showMessage("Không nhận diện được giọng nói.");
                            return;
                        }

                        applyVoiceResult(matches.get(0));
                    }
            );

    // -------------------------------------------------------------------------
    // Fragment Lifecycle
    // -------------------------------------------------------------------------

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;

        db = FirebaseFirestore.getInstance();
        historyRepository = new SearchHistoryRepository();
        userRepository = new UserRepository();
        pantryRepository = new PantryRepository();

        initViews(view);
        setupRecyclerView();
        setupVoiceHelper();
        setupRecentSearchChips();
        fetchUserProfile();

        buttonSearch.setOnClickListener(v -> performSearch());
        buttonUsePantry.setOnClickListener(v -> usePantryIngredients());
        buttonVoice.setOnClickListener(v -> onVoiceButtonClicked());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Giải phóng SpeechRecognizer để tránh memory leak
        if (voiceInputHelper != null) {
            voiceInputHelper.destroy();
            voiceInputHelper = null;
        }
    }

    // -------------------------------------------------------------------------
    // View Initialization
    // -------------------------------------------------------------------------

    private void initViews(View view) {
        editIngredientsInput = view.findViewById(R.id.editIngredientsInput);
        buttonSearch        = view.findViewById(R.id.buttonSearchRecipes);
        buttonUsePantry     = view.findViewById(R.id.buttonUsePantry);
        buttonVoice         = view.findViewById(R.id.buttonVoiceSearch);
        textVoiceStatus     = view.findViewById(R.id.textVoiceStatus);
        textClearRecentSearch = view.findViewById(R.id.textClearRecentSearch);
        progressSearch      = view.findViewById(R.id.progressSearch);
        recyclerResults     = view.findViewById(R.id.recyclerSearchResults);
        textResultsTitle    = view.findViewById(R.id.textResultsTitle);
        layoutStates        = view.findViewById(R.id.layoutSearchStates);
        layoutRecentSearchContainer = view.findViewById(R.id.layoutRecentSearchContainer);
        layoutRecentSearchChips = view.findViewById(R.id.layoutRecentSearchChips);
        textSearchState     = view.findViewById(R.id.textSearchState);
    }

    private void setupRecentSearchChips() {
        editIngredientsInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && isSearchInputEmpty()) {
                renderRecentSearchChips();
            }
        });

        editIngredientsInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)) {
                    renderRecentSearchChips();
                } else {
                    hideRecentSearchChips();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        textClearRecentSearch.setOnClickListener(v -> {
            SearchKeywordStore.clear(requireContext());
            hideRecentSearchChips();
        });
    }

    private void setupRecyclerView() {
        recipeAdapter = new RecipeAdapter(new ArrayList<>(), recipe -> {
            Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getRecipeId());
            startActivity(intent);
        });
        recyclerResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerResults.setAdapter(recipeAdapter);
    }

    private void renderRecentSearchChips() {
        if (!isAdded() || layoutRecentSearchContainer == null || layoutRecentSearchChips == null) {
            return;
        }

        List<String> keywords = SearchKeywordStore.getKeywords(requireContext());
        layoutRecentSearchChips.removeAllViews();
        if (keywords.isEmpty()) {
            hideRecentSearchChips();
            return;
        }

        for (String keyword : keywords) {
            TextView chip = new TextView(requireContext());
            chip.setText(keyword);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green));
            chip.setTextSize(12);
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
            chip.setBackgroundResource(R.drawable.bg_recipe_chip);
            chip.setPadding(dp(12), dp(7), dp(12), dp(7));
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> {
                editIngredientsInput.setText(keyword);
                editIngredientsInput.setSelection(keyword.length());
                hideRecentSearchChips();
                performSearch();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(8));
            layoutRecentSearchChips.addView(chip, params);
        }
        layoutRecentSearchContainer.setVisibility(View.VISIBLE);
    }

    private void hideRecentSearchChips() {
        if (layoutRecentSearchContainer != null) {
            layoutRecentSearchContainer.setVisibility(View.GONE);
        }
    }

    private boolean isSearchInputEmpty() {
        return editIngredientsInput == null || editIngredientsInput.getText() == null
                || TextUtils.isEmpty(editIngredientsInput.getText().toString().trim());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void usePantryIngredients() {
        if (!isAdded()) return;
        buttonUsePantry.setEnabled(false);
        pantryRepository.loadCurrentUserPantry(new PantryRepository.PantryCallback() {
            @Override
            public void onSuccess(List<String> ingredients) {
                if (!isAdded()) return;
                buttonUsePantry.setEnabled(true);
                if (ingredients == null || ingredients.isEmpty()) {
                    showMessage("Tủ lạnh chưa có nguyên liệu. Hãy thêm trong Hồ sơ.");
                    return;
                }

                String text = joinIngredients(ingredients);
                editIngredientsInput.setText(text);
                editIngredientsInput.setSelection(text.length());
                textVoiceStatus.setText("Đã dùng nguyên liệu từ tủ lạnh: " + text);
                hideRecentSearchChips();
                performSearch();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                buttonUsePantry.setEnabled(true);
                showMessage("Không thể tải tủ lạnh: " + e.getMessage());
            }

            @Override
            public void onLoginRequired() {
                if (!isAdded()) return;
                buttonUsePantry.setEnabled(true);
                showMessage("Vui lòng đăng nhập để dùng Tủ lạnh của tôi");
            }
        });
    }

    // -------------------------------------------------------------------------
    // Voice Input Setup
    // -------------------------------------------------------------------------

    private void setupVoiceHelper() {
        if (!VoiceInputHelper.isAvailable(requireContext())) {
            // Ẩn nút micro nếu thiết bị không hỗ trợ
            if (buttonVoice != null) buttonVoice.setVisibility(View.GONE);
            if (textVoiceStatus != null) textVoiceStatus.setText(R.string.voice_not_supported);
            return;
        }

        voiceInputHelper = new VoiceInputHelper(requireContext(), new VoiceInputHelper.VoiceInputCallback() {

            @Override
            public void onReady() {
                setVoiceListeningState(true, getString(R.string.voice_ready));
            }

            @Override
            public void onListening() {
                setVoiceListeningState(true, getString(R.string.voice_listening));
            }

            @Override
            public void onResult(String text) {
                applyVoiceResult(text);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                showMessage(message);
            }

            @Override
            public void onEnd() {
                // Trả UI về trạng thái bình thường
                setVoiceListeningState(false, getString(R.string.voice_hint));
            }
        });
    }

    // -------------------------------------------------------------------------
    // Voice Button Click Handler
    // -------------------------------------------------------------------------

    private void onVoiceButtonClicked() {
        Log.d("VoiceInputHelper", "Mic clicked");
        if (voiceInputHelper == null) {
            showMessage(getString(R.string.voice_not_supported));
            return;
        }

        // Nếu đang nghe thì dừng
        if (voiceInputHelper.isListening()) {
            Log.d("VoiceInputHelper", "Stopping listening (was active)");
            voiceInputHelper.stopListening();
            return;
        }

        // Ẩn bàn phím và bỏ focus
        hideKeyboard();
        editIngredientsInput.clearFocus();

        // Kiểm tra quyền RECORD_AUDIO
        Log.d("VoiceInputHelper", "Checking RECORD_AUDIO permission");
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.d("VoiceInputHelper", "Permission already granted, starting voice input");
            startSystemVoiceInput();
        } else {
            Log.d("VoiceInputHelper", "Requesting RECORD_AUDIO permission");
            requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
        }
    }

    private void hideKeyboard() {
        View view = getActivity() != null ? getActivity().getCurrentFocus() : null;
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void startVoiceInput() {
        if (voiceInputHelper != null) {
            Log.d("VoiceInputHelper", "startListening() called from fragment");
            voiceInputHelper.startListening();
        }
    }

    private void startSystemVoiceInput() {
        try {
            setVoiceListeningState(true, getString(R.string.voice_listening));
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN");
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói nguyên liệu bạn đang có");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            speechInputLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Log.d("VoiceInputHelper", "System recognizer not found, fallback helper");
            startVoiceInput();
        } catch (Exception e) {
            setVoiceListeningState(false, getString(R.string.voice_hint));
            showMessage("Không mở được nhận diện giọng nói: " + e.getMessage());
        }
    }

    private void applyVoiceResult(String rawText) {
        if (!isAdded()) return;
        String normalizedText = normalizeVoiceIngredients(rawText);
        String ingredientKeywords = extractIngredientKeywordsFromVoice(rawText);
        if (!ingredientKeywords.isEmpty()) {
            normalizedText = ingredientKeywords;
        }

        if (normalizedText.trim().isEmpty()) {
            showMessage("Không nhận diện được nguyên liệu.");
            return;
        }

        editIngredientsInput.setText(normalizedText);
        editIngredientsInput.setSelection(normalizedText.length());
        textVoiceStatus.setText("Đã nghe: " + normalizedText);
        performSearch();
    }

    /** Cập nhật UI nút micro theo trạng thái đang nghe / bình thường. */
    private void setVoiceListeningState(boolean isListening, String statusText) {
        if (!isAdded()) return;

        // Cập nhật text trạng thái
        if (textVoiceStatus != null) {
            if (isListening) {
                textVoiceStatus.setText("Đang nghe... Hãy nói nguyên liệu");
            } else {
                textVoiceStatus.setText(statusText);
            }
        }

        // Disable nút tìm kiếm khi đang nghe để tránh xung đột
        if (buttonSearch != null) {
            buttonSearch.setEnabled(!isListening);
        }

        if (buttonVoice != null) {
            if (isListening) {
                buttonVoice.setBackground(androidx.core.content.ContextCompat.getDrawable(
                        requireContext(), R.drawable.bg_mic_button_active));
                buttonVoice.setImageTintList(
                        android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)));
            } else {
                buttonVoice.setBackground(androidx.core.content.ContextCompat.getDrawable(
                        requireContext(), R.drawable.bg_mic_button));
                buttonVoice.setImageTintList(
                        android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_green)));
            }
        }
    }

    // -------------------------------------------------------------------------
    // User Profile (B11)
    // -------------------------------------------------------------------------

    private void fetchUserProfile() {
        if (FirebaseAuth.getInstance().getUid() == null) return;

        userRepository.getCurrentUserProfile(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    healthGoal    = user.getHealthGoal()    != null ? user.getHealthGoal()    : "";
                    userBudget    = user.getMealBudget()    != null ? user.getMealBudget()    : 0.0;
                    availableTime = user.getAvailableTime() != null ? user.getAvailableTime() : 0;
                }
            }

            @Override
            public void onError(Exception e) {
                // Giữ giá trị mặc định nếu lỗi — không ảnh hưởng đến luồng tìm kiếm
            }
        });
    }

    // -------------------------------------------------------------------------
    // Search Logic (B10 — giữ nguyên, không đổi)
    // -------------------------------------------------------------------------

    private void performSearch() {
        if (!isAdded()) return;

        String input = editIngredientsInput.getText() != null
                ? editIngredientsInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(input)) {
            showMessage("Vui lòng nhập nguyên liệu");
            return;
        }

        SearchKeywordStore.saveKeyword(requireContext(), input);
        hideRecentSearchChips();

        List<String> userIngredients = parseIngredients(input);

        setLoading(true);

        db.collection("recipes")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    // Dùng Map để lọc trùng tên món
                    Map<String, RecommendationEngine.RecipeWithScore> uniqueRecipes = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Recipe recipe = RecipeRepository.mapRecipeDocument(doc);

                            String normalizedName = recipe.getName() != null
                                    ? RecommendationEngine.normalizeVietnamese(recipe.getName()) : "";
                            if (normalizedName.isEmpty()) continue;

                            double ingredientScore = RecommendationEngine.calculateIngredientScore(
                                    recipe, userIngredients);

                            if (ingredientScore > 0) {
                                ArrayList<String> matchedIngredients =
                                        RecommendationEngine.findMatchedIngredients(recipe, userIngredients);
                                ArrayList<String> missingIngredients =
                                        RecommendationEngine.findMissingIngredients(recipe, userIngredients);
                                int matchPercent =
                                        RecommendationEngine.calculateMatchPercent(recipe, matchedIngredients);
                                double healthScore = RecommendationEngine.calculateHealthScore(
                                        recipe, healthGoal);
                                double budgetScore = RecommendationEngine.calculateBudgetScore(
                                        recipe, userBudget);
                                double timeScore   = RecommendationEngine.calculateTimeScore(
                                        recipe, availableTime);
                                double favoriteScore = 70.0;
                                double totalScore  = (ingredientScore * 0.40)
                                        + (healthScore  * 0.20)
                                        + (budgetScore  * 0.15)
                                        + (timeScore    * 0.15)
                                        + (favoriteScore * 0.10);

                                recipe.setMatchedIngredients(matchedIngredients);
                                recipe.setMissingIngredients(missingIngredients);
                                recipe.setMatchPercent(matchPercent);
                                recipe.setRecommendationScore(totalScore);
                                recipe.setIngredientScore(ingredientScore);
                                recipe.setHealthScore(healthScore);
                                recipe.setBudgetScore(budgetScore);
                                recipe.setTimeScore(timeScore);
                                recipe.setFavoriteScore(favoriteScore);
                                recipe.setConfidenceLevel(RecommendationEngine.getConfidenceLevel(totalScore));
                                recipe.setCookabilityLevel(RecommendationEngine.getCookabilityLevel(matchPercent));
                                recipe.setRecommendationReason(
                                        RecommendationEngine.buildRecommendationReason(
                                                recipe,
                                                matchedIngredients,
                                                missingIngredients,
                                                healthGoal,
                                                (int) Math.round(userBudget),
                                                availableTime));

                                if (!uniqueRecipes.containsKey(normalizedName)
                                        || totalScore > uniqueRecipes.get(normalizedName).score) {
                                    uniqueRecipes.put(normalizedName,
                                            new RecommendationEngine.RecipeWithScore(recipe, totalScore));
                                }
                            }
                        } catch (Exception e) {
                            Log.e("SearchFragment", "Error mapping recipe: " + e.getMessage());
                        }
                    }

                    List<RecommendationEngine.RecipeWithScore> scoredRecipes =
                            new ArrayList<>(uniqueRecipes.values());
                    RecommendationEngine.sortRecipesByScore(scoredRecipes);

                    List<Recipe> finalRecipes = new ArrayList<>();
                    for (RecommendationEngine.RecipeWithScore rws : scoredRecipes) {
                        finalRecipes.add(rws.recipe);
                    }

                    setLoading(false);
                    displayResults(finalRecipes);

                    // Lưu lịch sử tìm kiếm (B10)
                    historyRepository.saveSearchHistory(input, userIngredients);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    setLoading(false);
                    showErrorState("Lỗi kết nối: " + e.getMessage());
                });
    }

    // -------------------------------------------------------------------------
    // UI State Helpers
    // -------------------------------------------------------------------------

    private void setLoading(boolean isLoading) {
        progressSearch.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerResults.setVisibility(View.GONE);
        layoutStates.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        textResultsTitle.setVisibility(View.GONE);
        buttonSearch.setEnabled(!isLoading);
    }

    private void displayResults(List<Recipe> recipes) {
        if (!isAdded()) return;
        if (recipes.isEmpty()) {
            showNoResultState();
        } else {
            recipeAdapter.submitList(recipes);
            recyclerResults.setVisibility(View.VISIBLE);
            layoutStates.setVisibility(View.GONE);
            textResultsTitle.setVisibility(View.VISIBLE);
        }
    }

    private void showNoResultState() {
        textSearchState.setText(getString(R.string.search_no_result));
        layoutStates.setVisibility(View.VISIBLE);
        recyclerResults.setVisibility(View.GONE);
        textResultsTitle.setVisibility(View.GONE);
    }

    private void showErrorState(String error) {
        textSearchState.setText(getString(R.string.search_error) + ": " + error);
        layoutStates.setVisibility(View.VISIBLE);
        recyclerResults.setVisibility(View.GONE);
        textResultsTitle.setVisibility(View.GONE);
    }

    private void showMessage(String message) {
        if (!isAdded() || rootView == null) {
            return;
        }
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }

    private String normalizeVoiceIngredients(String rawText) {
        if (rawText == null) {
            return "";
        }

        String normalized = rawText.toLowerCase()
                .replace("nguyên liệu", " ")
                .replace("tôi có", " ")
                .replace("mình có", " ")
                .replace("muốn ăn", " ")
                .replace("tìm món", " ")
                .replace("gợi ý món", " ")
                .replace("ăn", " ")
                .replace("có", " ")
                .replaceAll("\\b(với|và|cùng|kèm|thêm|rồi|nữa)\\b", ",")
                .replaceAll("\\s+", " ")
                .replaceAll("[.;\\n]+", ",")
                .replaceAll("\\s*,\\s*", ", ")
                .replaceAll("(,\\s*)+", ", ")
                .trim();

        while (normalized.startsWith(",")) {
            normalized = normalized.substring(1).trim();
        }
        while (normalized.endsWith(",")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private String extractIngredientKeywordsFromVoice(String rawText) {
        String normalizedVoice = RecommendationEngine.normalizeVietnamese(rawText);
        if (normalizedVoice.isEmpty()) {
            return "";
        }

        List<String> ingredients = new ArrayList<>();
        for (String ingredient : COMMON_SEARCH_INGREDIENTS) {
            String normalizedIngredient = RecommendationEngine.normalizeVietnamese(ingredient);
            if (normalizedVoice.contains(normalizedIngredient)
                    && !containsNormalized(ingredients, ingredient)) {
                ingredients.add(ingredient);
            }
        }

        for (String token : normalizedVoice.split("[^a-z0-9]+")) {
            if (token.length() < 2 || isStopword(token)) {
                continue;
            }

            for (String ingredient : COMMON_SEARCH_INGREDIENTS) {
                String normalizedIngredient = RecommendationEngine.normalizeVietnamese(ingredient);
                if (normalizedIngredient.contains(" ")) {
                    continue;
                }

                if (isLikelyVoiceMatch(token, normalizedIngredient)
                        && !containsNormalized(ingredients, ingredient)) {
                    ingredients.add(ingredient);
                    break;
                }
            }
        }

        return joinIngredients(ingredients);
    }

    private boolean isLikelyVoiceMatch(String token, String ingredient) {
        if (token.equals(ingredient)) {
            return true;
        }

        // Nguyên liệu quá ngắn như "cá" chỉ nhận khi khớp chính xác,
        // tránh "cà chua" bị hiểu thêm thành "cá".
        if (ingredient.length() <= 2) {
            return false;
        }

        // Google Speech đôi khi nghe "cơm" thành "công" hoặc "con".
        if ("com".equals(ingredient)
                && ("cong".equals(token) || "con".equals(token) || "gom".equals(token))) {
            return true;
        }

        if (token.length() < 3 || ingredient.length() < 3) {
            return false;
        }
        return levenshteinDistance(token, ingredient) <= 1;
    }

    private boolean isStopword(String value) {
        for (String stopword : SEARCH_STOPWORDS) {
            if (value.equals(RecommendationEngine.normalizeVietnamese(stopword))) {
                return true;
            }
        }
        return false;
    }

    private int levenshteinDistance(String first, String second) {
        int[][] dp = new int[first.length() + 1][second.length() + 1];
        for (int i = 0; i <= first.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= second.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= first.length(); i++) {
            for (int j = 1; j <= second.length(); j++) {
                int cost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[first.length()][second.length()];
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

    private List<String> parseIngredients(String input) {
        String normalizedInput = normalizeVoiceIngredients(input);
        String[] splitInputs = normalizedInput.split("[,;\\n]+");
        List<String> userIngredients = new ArrayList<>();

        for (String s : splitInputs) {
            String trimmed = s.trim();
            if (isValidSearchIngredient(trimmed) && !containsNormalized(userIngredients, trimmed)) {
                userIngredients.add(trimmed);
            }
        }

        if (userIngredients.size() == 1 && userIngredients.get(0).contains(" ")) {
            String originalPhrase = userIngredients.get(0);
            for (String word : userIngredients.get(0).split("\\s+")) {
                String trimmed = word.trim();
                if (isValidSearchIngredient(trimmed) && !containsNormalized(userIngredients, trimmed)) {
                    userIngredients.add(trimmed);
                }
            }
            if (!isUsefulPhrase(originalPhrase) && userIngredients.size() > 1) {
                userIngredients.remove(0);
            }
        }

        return userIngredients;
    }

    private boolean isValidSearchIngredient(String value) {
        String normalized = RecommendationEngine.normalizeVietnamese(value);
        if (normalized.length() < 2) {
            return false;
        }

        for (String stopword : SEARCH_STOPWORDS) {
            if (normalized.equals(RecommendationEngine.normalizeVietnamese(stopword))) {
                return false;
            }
        }
        return true;
    }

    private boolean isUsefulPhrase(String value) {
        String normalized = RecommendationEngine.normalizeVietnamese(value);
        if (normalized.length() < 2) {
            return false;
        }

        for (String word : normalized.split("\\s+")) {
            for (String stopword : SEARCH_STOPWORDS) {
                if (word.equals(RecommendationEngine.normalizeVietnamese(stopword))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean containsNormalized(List<String> values, String candidate) {
        String normalizedCandidate = RecommendationEngine.normalizeVietnamese(candidate);
        for (String value : values) {
            if (RecommendationEngine.normalizeVietnamese(value).equals(normalizedCandidate)) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Firestore Document Mapping (Đã chuyển sang toObject trong performSearch)
    // -------------------------------------------------------------------------

    private Recipe mapDocumentToRecipe(DocumentSnapshot document) {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(document.getId());
        recipe.setName(document.getString("name"));
        recipe.setDescription(document.getString("description"));
        recipe.setImageUrl(document.getString("imageUrl"));

        Object caloriesObj = document.get("calories");
        recipe.setCalories(caloriesObj instanceof Number ? ((Number) caloriesObj).intValue() : 0);

        Object costObj = document.get("estimatedCost");
        recipe.setEstimatedCost(costObj instanceof Number ? ((Number) costObj).doubleValue() : 0.0);

        Object timeObj = document.get("cookingTime");
        recipe.setCookingTime(timeObj instanceof Number ? ((Number) timeObj).intValue() : 0);

        recipe.setIngredients(toStringList(document.get("ingredients")));
        recipe.setSteps(toStringList(document.get("steps")));

        return recipe;
    }

    private List<String> toStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item != null) result.add(item.toString());
            }
        }
        return result;
    }
}
