package hung.edu.mealmindai.repositories;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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
                    List<Recipe> recipes = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        recipes.add(mapDocumentToRecipe(document));
                    }
                    callback.onSuccess(recipes);
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
        recipe.setImageUrl(document.getString("imageUrl"));
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
