package hung.edu.mealmindai.repositories;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hung.edu.mealmindai.models.ChatMessage;
import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.models.RecipeScoreResult;
import hung.edu.mealmindai.models.SmartSuggestionResponse;
import hung.edu.mealmindai.models.User;
import hung.edu.mealmindai.utils.SmartMealAssistant;

public class SmartSuggestionRepository {
    private static final String TAG = "SmartMealAI";
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SmartMealAssistant assistant = new SmartMealAssistant();

    public interface SuggestionCallback {
        void onSuccess(SmartSuggestionResponse response);
        void onError(Exception e);
        void onLoginRequired();
    }

    public interface HistoryCallback {
        void onSuccess(List<ChatMessage> messages);
        void onError(Exception e);
        void onLoginRequired();
    }

    public void loadDataAndSuggest(String input, SuggestionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onLoginRequired();
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDocument -> {
                    User user = mapUser(userDocument, currentUser);
                    loadRecipesAndSuggest(currentUser.getUid(), input, user, callback);
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "load user profile error: " + e.getMessage());
                    loadRecipesAndSuggest(currentUser.getUid(), input, new User(), callback);
                });
    }

    private void loadRecipesAndSuggest(String userId, String input, User user, SuggestionCallback callback) {
        db.collection("recipes")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        try {
                            recipes.add(RecipeRepository.mapRecipeDocument(document));
                        } catch (Exception e) {
                            Log.d(TAG, "skip recipe map error: " + e.getMessage());
                        }
                    }
                    SmartSuggestionResponse response = assistant.suggestMeals(input, user, recipes);
                    saveAiSuggestionHistory(userId, input, response);
                    callback.onSuccess(response);
                })
                .addOnFailureListener(callback::onError);
    }

    public void saveAiSuggestionHistory(String userId, String inputText, SmartSuggestionResponse response) {
        ArrayList<String> recipeIds = new ArrayList<>();
        if (response != null && response.getResults() != null) {
            for (RecipeScoreResult result : response.getResults()) {
                if (result.getRecipe() != null && result.getRecipe().getRecipeId() != null) {
                    recipeIds.add(result.getRecipe().getRecipeId());
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("inputText", inputText);
        data.put("responseText", response != null ? response.getResponseText() : "");
        data.put("suggestedRecipeIds", recipeIds);
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("aiSuggestionHistory")
                .add(data)
                .addOnSuccessListener(documentReference ->
                        Log.d(TAG, "save history success: " + documentReference.getId()))
                .addOnFailureListener(e ->
                        Log.d(TAG, "save history error: " + e.getMessage()));
    }

    public void loadRecentChatHistory(HistoryCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onLoginRequired();
            return;
        }

        db.collection("aiSuggestionHistory")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> documents = new ArrayList<>(snapshot.getDocuments());
                    Collections.sort(documents, (first, second) ->
                            Long.compare(getCreatedAtMillis(first), getCreatedAtMillis(second)));

                    int startIndex = Math.max(0, documents.size() - 10);
                    List<ChatMessage> messages = new ArrayList<>();
                    for (int i = startIndex; i < documents.size(); i++) {
                        DocumentSnapshot document = documents.get(i);
                        long createdAt = getCreatedAtMillis(document);
                        String inputText = document.getString("inputText");
                        String responseText = document.getString("responseText");

                        if (inputText != null && !inputText.trim().isEmpty()) {
                            messages.add(new ChatMessage(
                                    document.getId() + "_user",
                                    inputText,
                                    "user",
                                    createdAt
                            ));
                        }
                        if (responseText != null && !responseText.trim().isEmpty()) {
                            messages.add(new ChatMessage(
                                    document.getId() + "_ai",
                                    responseText,
                                    "ai",
                                    createdAt + 1
                            ));
                        }
                    }
                    callback.onSuccess(messages);
                })
                .addOnFailureListener(callback::onError);
    }

    private User mapUser(DocumentSnapshot document, FirebaseUser authUser) {
        User user = new User();
        user.setUid(authUser.getUid());
        user.setEmail(authUser.getEmail());
        if (document != null && document.exists()) {
            user.setFullName(document.getString("fullName"));
            user.setHealthGoal(firstNonEmpty(document.getString("healthGoal"), document.getString("goal")));
            user.setGoal(document.getString("goal"));
            user.setMealBudget(toDouble(document.get("mealBudget")));
            user.setAvailableTime(toInteger(document.get("availableTime")));
        }
        return user;
    }

    private String firstNonEmpty(String first, String second) {
        return first != null && !first.trim().isEmpty() ? first : second;
    }

    private Double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    private Integer toInteger(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private long getCreatedAtMillis(DocumentSnapshot document) {
        Timestamp timestamp = document.getTimestamp("createdAt");
        return timestamp != null ? timestamp.toDate().getTime() : 0L;
    }
}
