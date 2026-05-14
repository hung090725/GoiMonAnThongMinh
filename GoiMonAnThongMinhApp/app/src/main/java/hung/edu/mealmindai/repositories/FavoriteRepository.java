package hung.edu.mealmindai.repositories;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hung.edu.mealmindai.models.Recipe;

/**
 * Repository xử lý toàn bộ logic yêu thích món ăn với Firestore.
 */
public class FavoriteRepository {

    private static final String TAG = "FavoriteRepository";
    private static final String COLLECTION_FAVORITES = "favorites";
    private static final String COLLECTION_RECIPES = "recipes";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FavoriteRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // -------------------------------------------------------------------------
    // Callback Interfaces
    // -------------------------------------------------------------------------

    public interface FavoriteCheckCallback {
        void onResult(boolean isFavorited, String favoriteId);
        void onError(Exception e);
    }

    public interface FavoriteActionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface FavoriteRecipesCallback {
        void onSuccess(List<Recipe> recipes);
        void onError(Exception e);
    }

    // -------------------------------------------------------------------------
    // Public Methods
    // -------------------------------------------------------------------------

    /**
     * Kiểm tra xem user hiện tại đã yêu thích món ăn này chưa.
     */
    public void checkFavorite(String recipeId, FavoriteCheckCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onResult(false, null);
            return;
        }

        if (recipeId == null || recipeId.isEmpty()) {
            callback.onResult(false, null);
            return;
        }

        db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("recipeId", recipeId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String favoriteId = querySnapshot.getDocuments().get(0).getId();
                        callback.onResult(true, favoriteId);
                    } else {
                        callback.onResult(false, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi kiểm tra yêu thích: " + e.getMessage(), e);
                    callback.onError(e);
                });
    }

    /**
     * Thêm món ăn vào danh sách yêu thích.
     */
    public void addFavorite(String recipeId, FavoriteActionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("Vui lòng đăng nhập để lưu yêu thích"));
            return;
        }

        if (recipeId == null || recipeId.isEmpty()) {
            callback.onError(new Exception("Không tìm thấy mã món ăn"));
            return;
        }

        Map<String, Object> favoriteData = new HashMap<>();
        favoriteData.put("userId", currentUser.getUid());
        favoriteData.put("recipeId", recipeId);
        favoriteData.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_FAVORITES)
                .add(favoriteData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Đã thêm yêu thích: " + documentReference.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi thêm yêu thích: " + e.getMessage(), e);
                    callback.onError(e);
                });
    }

    /**
     * Xóa món ăn khỏi danh sách yêu thích.
     */
    public void removeFavorite(String favoriteId, FavoriteActionCallback callback) {
        if (favoriteId == null || favoriteId.isEmpty()) {
            callback.onError(new Exception("Không tìm thấy mã yêu thích"));
            return;
        }

        db.collection(COLLECTION_FAVORITES)
                .document(favoriteId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Đã bỏ yêu thích: " + favoriteId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi bỏ yêu thích: " + e.getMessage(), e);
                    callback.onError(e);
                });
    }

    /**
     * Tải danh sách món ăn yêu thích của user hiện tại.
     * Bước 1: Lấy tất cả favoriteId + recipeId từ collection favorites
     * Bước 2: Với mỗi recipeId, đọc thông tin chi tiết từ collection recipes
     * Bước 3: Trả về danh sách Recipe hoàn chỉnh
     */
    public void loadFavoriteRecipes(FavoriteRecipesCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<String> recipeIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String recipeId = doc.getString("recipeId");
                        if (recipeId != null && !recipeId.isEmpty()) {
                            recipeIds.add(recipeId);
                        }
                    }

                    if (recipeIds.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    fetchRecipesByIds(recipeIds, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải danh sách yêu thích: " + e.getMessage(), e);
                    callback.onError(e);
                });
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    /**
     * Đọc chi tiết các món ăn theo danh sách recipeId.
     * Sử dụng counter để đồng bộ các request bất đồng bộ.
     */
    private void fetchRecipesByIds(List<String> recipeIds, FavoriteRecipesCallback callback) {
        List<Recipe> recipes = new ArrayList<>();
        final int[] completedCount = {0};
        final int totalCount = recipeIds.size();

        for (String recipeId : recipeIds) {
            db.collection(COLLECTION_RECIPES)
                    .document(recipeId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Recipe recipe = mapDocumentToRecipe(documentSnapshot);
                            synchronized (recipes) {
                                recipes.add(recipe);
                            }
                        }
                        completedCount[0]++;
                        if (completedCount[0] == totalCount) {
                            callback.onSuccess(recipes);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi đọc recipe " + recipeId + ": " + e.getMessage());
                        completedCount[0]++;
                        if (completedCount[0] == totalCount) {
                            callback.onSuccess(recipes);
                        }
                    });
        }
    }

    private Recipe mapDocumentToRecipe(DocumentSnapshot document) {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(document.getId());
        recipe.setName(document.getString("name"));
        recipe.setTitle(document.getString("name"));
        recipe.setDescription(document.getString("description"));
        recipe.setImageUrl(document.getString("imageUrl"));
        recipe.setCategoryId(document.getString("categoryId"));

        Object caloriesObj = document.get("calories");
        recipe.setCalories(caloriesObj instanceof Number ? ((Number) caloriesObj).intValue() : 0);

        Object costObj = document.get("estimatedCost");
        recipe.setEstimatedCost(costObj instanceof Number ? ((Number) costObj).doubleValue() : 0.0);

        Object timeObj = document.get("cookingTime");
        recipe.setCookingTime(timeObj instanceof Number ? ((Number) timeObj).intValue() : 0);

        recipe.setDifficulty(document.getString("difficulty"));
        recipe.setAuthorId(document.getString("authorId"));
        recipe.setAuthorName(document.getString("authorName"));
        recipe.setStatus(document.getString("status"));

        Object likeCountObj = document.get("likeCount");
        recipe.setLikeCount(likeCountObj instanceof Number ? ((Number) likeCountObj).intValue() : 0);

        recipe.setIngredients(toStringList(document.get("ingredients")));
        recipe.setSteps(toStringList(document.get("steps")));

        return recipe;
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
