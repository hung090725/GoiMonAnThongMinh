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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        checkFavorite(recipeId, new FavoriteCheckCallback() {
            @Override
            public void onResult(boolean isFavorited, String favoriteId) {
                if (isFavorited) {
                    callback.onSuccess();
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

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void removeFavoriteByRecipeId(String recipeId, FavoriteActionCallback callback) {
        checkFavorite(recipeId, new FavoriteCheckCallback() {
            @Override
            public void onResult(boolean isFavorited, String favoriteId) {
                if (!isFavorited) {
                    callback.onSuccess();
                    return;
                }
                removeFavorite(favoriteId, callback);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

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

                    Set<String> recipeIds = new LinkedHashSet<>();
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

                    fetchRecipesByIds(new ArrayList<>(recipeIds), callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải danh sách yêu thích: " + e.getMessage(), e);
                    callback.onError(e);
                });
    }

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
                            try {
                                Recipe recipe = RecipeRepository.mapRecipeDocument(documentSnapshot);
                                if (recipe.getName() != null || recipe.getTitle() != null) {
                                    synchronized (recipes) {
                                        recipes.add(recipe);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error deserializing recipe: " + e.getMessage());
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
}
