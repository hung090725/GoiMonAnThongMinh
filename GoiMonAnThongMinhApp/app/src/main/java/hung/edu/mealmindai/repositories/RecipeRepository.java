package hung.edu.mealmindai.repositories;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hung.edu.mealmindai.models.Recipe;

public class RecipeRepository {
    private static final String TAG = "RecipeRepository";
    private final FirebaseFirestore firestore;

    public RecipeRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public interface RecipeCallback {
        void onSuccess(List<Recipe> recipes);
        void onError(Exception exception);
    }

    public void loadApprovedRecipes(RecipeCallback callback) {
        firestore.collection("recipes")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Recipe> uniqueRecipes = new LinkedHashMap<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        try {
                            Recipe recipe = mapRecipeDocument(document);
                            if (recipe.getName() != null || recipe.getTitle() != null) {
                                String uniqueKey = buildUniqueRecipeKey(recipe);
                                if (!uniqueRecipes.containsKey(uniqueKey)) {
                                    uniqueRecipes.put(uniqueKey, recipe);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error deserializing recipe: " + e.getMessage());
                        }
                    }
                    callback.onSuccess(new ArrayList<>(uniqueRecipes.values()));
                })
                .addOnFailureListener(callback::onError);
    }

    public void loadRecipesByIds(List<String> recipeIds, RecipeCallback callback) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<String> safeRecipeIds = new ArrayList<>();
        for (String recipeId : recipeIds) {
            if (recipeId != null && !recipeId.trim().isEmpty() && !safeRecipeIds.contains(recipeId)) {
                safeRecipeIds.add(recipeId);
            }
        }

        if (safeRecipeIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        Map<String, Recipe> recipesById = new LinkedHashMap<>();
        final int[] remaining = {safeRecipeIds.size()};
        final Exception[] firstError = {null};

        for (String recipeId : safeRecipeIds) {
            firestore.collection("recipes")
                    .document(recipeId)
                    .get()
                    .addOnSuccessListener(document -> {
                        try {
                            if (document.exists()) {
                                Recipe recipe = mapRecipeDocument(document);
                                if ("approved".equals(recipe.getStatus())) {
                                    recipesById.put(recipe.getRecipeId(), recipe);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error mapping recent recipe: " + e.getMessage());
                            if (firstError[0] == null) {
                                firstError[0] = e;
                            }
                        }
                        finishLoadRecipesByIds(safeRecipeIds, recipesById, remaining, firstError, callback);
                    })
                    .addOnFailureListener(e -> {
                        if (firstError[0] == null) {
                            firstError[0] = e;
                        }
                        finishLoadRecipesByIds(safeRecipeIds, recipesById, remaining, firstError, callback);
                    });
        }
    }

    private void finishLoadRecipesByIds(
            List<String> recipeIds,
            Map<String, Recipe> recipesById,
            int[] remaining,
            Exception[] firstError,
            RecipeCallback callback) {
        remaining[0]--;
        if (remaining[0] > 0) {
            return;
        }

        if (recipesById.isEmpty() && firstError[0] != null) {
            callback.onError(firstError[0]);
            return;
        }

        List<Recipe> orderedRecipes = new ArrayList<>();
        for (String id : recipeIds) {
            Recipe recipe = recipesById.get(id);
            if (recipe != null) {
                orderedRecipes.add(recipe);
            }
        }
        callback.onSuccess(orderedRecipes);
    }

    public void submitRecipeForReview(Recipe recipe, RecipeActionCallback callback) {
        if (recipe == null) {
            callback.onError(new Exception("Công thức không hợp lệ"));
            return;
        }

        firestore.collection("recipes")
                .add(buildPendingRecipeData(recipe))
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Đã gửi món ăn chờ duyệt: " + documentReference.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onError);
    }

    public interface RecipeActionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    private Map<String, Object> buildPendingRecipeData(Recipe recipe) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", safeString(recipe.getName()));
        data.put("title", safeString(recipe.getTitle()));
        data.put("description", safeString(recipe.getDescription()));
        data.put("imageUrl", safeString(recipe.getImageUrl()));
        data.put("calories", recipe.getCalories() != null ? recipe.getCalories() : 0);
        data.put("estimatedCost", recipe.getEstimatedCost() != null ? recipe.getEstimatedCost() : 0.0);
        data.put("cookingTime", recipe.getCookingTime() != null ? recipe.getCookingTime() : 0);
        data.put("difficulty", safeString(recipe.getDifficulty()));
        data.put("categoryId", safeString(recipe.getCategoryId()));
        data.put("ingredients", recipe.getIngredients() != null ? recipe.getIngredients() : new ArrayList<String>());
        data.put("steps", recipe.getSteps() != null ? recipe.getSteps() : new ArrayList<String>());
        data.put("authorId", safeString(recipe.getAuthorId()));
        data.put("authorName", safeString(recipe.getAuthorName()));
        data.put("status", "pending");
        data.put("likeCount", 0);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String buildUniqueRecipeKey(Recipe recipe) {
        String name = recipe.getName();
        if (name != null && !name.trim().isEmpty()) {
            return name.trim().toLowerCase();
        }
        return recipe.getRecipeId();
    }

    public static Recipe mapRecipeDocument(DocumentSnapshot document) {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(firstNonEmpty(document.getString("recipeId"), document.getId()));
        recipe.setName(firstNonEmpty(document.getString("name"), document.getString("title")));
        recipe.setTitle(firstNonEmpty(document.getString("title"), document.getString("name")));
        recipe.setDescription(document.getString("description"));
        recipe.setImageUrl(document.getString("imageUrl"));
        recipe.setCategoryId(document.getString("categoryId"));
        recipe.setIngredients(toStringList(document.get("ingredients")));
        recipe.setSteps(toStringList(document.get("steps")));
        recipe.setCalories(toInteger(document.get("calories")));
        recipe.setEstimatedCost(toDouble(document.get("estimatedCost")));
        recipe.setCookingTime(firstNonNullInteger(document.get("cookingTime"), document.get("cookingTimeMinutes")));
        recipe.setCookingTimeMinutes(toInteger(document.get("cookingTimeMinutes")));
        recipe.setProtein(toDouble(document.get("protein")));
        recipe.setCarbs(toDouble(document.get("carbs")));
        recipe.setFat(toDouble(document.get("fat")));
        recipe.setServingSize(toInteger(document.get("servingSize")));
        recipe.setDifficulty(document.getString("difficulty"));
        recipe.setTags(toStringList(document.get("tags")));
        recipe.setAuthorId(firstNonEmpty(document.getString("authorId"), document.getString("createdBy")));
        recipe.setAuthorName(document.getString("authorName"));
        recipe.setStatus(document.getString("status"));
        recipe.setLikeCount(toInteger(document.get("likeCount")));
        recipe.setCreatedBy(document.getString("createdBy"));
        recipe.setAiGenerated(document.getBoolean("aiGenerated"));
        recipe.setCreatedAt(document.getTimestamp("createdAt"));
        recipe.setUpdatedAt(document.getTimestamp("updatedAt"));
        recipe.setReviewedAt(document.getTimestamp("reviewedAt"));
        recipe.setReviewedBy(document.getString("reviewedBy"));
        recipe.setRejectReason(document.getString("rejectReason"));
        return recipe;
    }

    private static Integer firstNonNullInteger(Object first, Object second) {
        Integer value = toInteger(first);
        return value != null ? value : toInteger(second);
    }

    private static String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }

    private static Integer toInteger(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private static Double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    private static List<String> toStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
        }
        return result;
    }
}
