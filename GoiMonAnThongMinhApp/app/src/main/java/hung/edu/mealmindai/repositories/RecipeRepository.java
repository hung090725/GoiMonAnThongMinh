package hung.edu.mealmindai.repositories;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hung.edu.mealmindai.models.Recipe;

public class RecipeRepository {

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
                        Recipe recipe = mapDocumentToRecipe(document);
                        String uniqueKey = buildUniqueRecipeKey(recipe);
                        if (!uniqueRecipes.containsKey(uniqueKey)) {
                            uniqueRecipes.put(uniqueKey, recipe);
                        }
                    }
                    callback.onSuccess(new ArrayList<>(uniqueRecipes.values()));
                })
                .addOnFailureListener(callback::onError);
    }

    private Recipe mapDocumentToRecipe(DocumentSnapshot document) {
        Recipe recipe = new Recipe();

        String recipeId = document.getString("recipeId");
        recipe.setRecipeId(isEmpty(recipeId) ? document.getId() : recipeId);
        recipe.setName(document.getString("name"));
        recipe.setTitle(document.getString("name"));
        recipe.setDescription(document.getString("description"));
        recipe.setImageUrl(resolveRecipeImage(recipe.getName(), document.getString("imageUrl")));
        recipe.setCategoryId(document.getString("categoryId"));
        recipe.setIngredients(toStringList(document.get("ingredients")));
        recipe.setSteps(toStringList(document.get("steps")));
        recipe.setCalories(toInteger(document.get("calories")));
        recipe.setEstimatedCost(toDouble(document.get("estimatedCost")));
        recipe.setCookingTime(toInteger(document.get("cookingTime")));
        recipe.setDifficulty(document.getString("difficulty"));
        recipe.setAuthorId(document.getString("authorId"));
        recipe.setAuthorName(document.getString("authorName"));
        recipe.setStatus(document.getString("status"));
        recipe.setLikeCount(toInteger(document.get("likeCount")));
        recipe.setCreatedAtTimestamp(document.getTimestamp("createdAt"));

        return recipe;
    }

    private String buildUniqueRecipeKey(Recipe recipe) {
        String name = recipe.getName();
        if (!isEmpty(name)) {
            return name.trim().toLowerCase();
        }
        return recipe.getRecipeId();
    }

    private String resolveRecipeImage(String recipeName, String currentImageUrl) {
        String sampleImageUrl = getSampleImageUrl(recipeName);
        if (!isEmpty(sampleImageUrl)) {
            return sampleImageUrl;
        }
        return currentImageUrl;
    }

    private String getSampleImageUrl(String recipeName) {
        if (isEmpty(recipeName)) {
            return null;
        }

        switch (recipeName.trim().toLowerCase()) {
            case "trứng sốt cà chua":
                return "res://trungsotcachua";
            case "cơm rang trứng":
                return "https://images.pexels.com/photos/28503599/pexels-photo-28503599.jpeg?auto=compress&cs=tinysrgb&w=1200";
            case "canh rau ngót thịt băm":
                return "https://images.pexels.com/photos/539451/pexels-photo-539451.jpeg?auto=compress&cs=tinysrgb&w=1200";
            case "salad ức gà":
                return "https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=1200";
            case "mì xào rau củ":
                return "https://images.pexels.com/photos/2347311/pexels-photo-2347311.jpeg?auto=compress&cs=tinysrgb&w=1200";
            case "đậu hũ sốt cà":
                return "res://dauhuca";
            case "cháo thịt băm":
                return "res://chaobam";
            case "cá kho tiêu":
                return "https://images.pexels.com/photos/262959/pexels-photo-262959.jpeg?auto=compress&cs=tinysrgb&w=1200";
            case "gà xào sả ớt":
                return "res://gaxaosa";
            case "rau luộc trứng":
                return "https://images.pexels.com/photos/257816/pexels-photo-257816.jpeg?auto=compress&cs=tinysrgb&w=1200";
            default:
                return null;
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private List<String> toStringList(Object value) {
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
