package hung.edu.mealmindai.repositories;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hung.edu.mealmindai.models.Recipe;

public class AdminRecipeRepository {
    private static final String TAG = "AdminRecipeRepository";
    private static final String ADMIN_FLOW_TAG = "AdminFlow";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public interface AdminCheckCallback {
        void onResult(boolean isAdmin);
        void onError(Exception e);
    }

    public interface StatsCallback {
        void onSuccess(DashboardStats stats);
        void onError(Exception e);
    }

    public interface RecipesCallback {
        void onSuccess(List<Recipe> recipes);
        void onError(Exception e);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public static class DashboardStats {
        public int totalRecipes;
        public int pendingRecipes;
        public int approvedRecipes;
        public int rejectedRecipes;
        public int hiddenRecipes;
        public int users;
        public int favorites;
        public int searchHistory;
    }

    public void checkCurrentUserIsAdmin(AdminCheckCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.d(ADMIN_FLOW_TAG, "check admin fail: currentUser is null");
            callback.onResult(false);
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(document -> {
                    String role = document.getString("role");
                    boolean isAdmin = "admin".equalsIgnoreCase(role);
                    Log.d(ADMIN_FLOW_TAG, "check admin success uid=" + user.getUid() + " isAdmin=" + isAdmin);
                    callback.onResult(isAdmin);
                })
                .addOnFailureListener(e -> {
                    Log.d(ADMIN_FLOW_TAG, "check admin error: " + e.getMessage());
                    callback.onError(e);
                });
    }

    public void loadDashboardStats(StatsCallback callback) {
        Log.d(ADMIN_FLOW_TAG, "load stats start");
        DashboardStats stats = new DashboardStats();
        final int[] completed = {0};
        final Exception[] firstError = {null};
        final int totalSteps = 8;

        loadRecipeCount("pending", count -> {
            stats.pendingRecipes = count;
            finishStatsStep(callback, stats, completed, firstError, null, totalSteps);
        }, e -> finishStatsStep(callback, stats, completed, firstError, e, totalSteps));

        loadRecipeCount("approved", count -> {
            stats.approvedRecipes = count;
            finishStatsStep(callback, stats, completed, firstError, null, totalSteps);
        }, e -> finishStatsStep(callback, stats, completed, firstError, e, totalSteps));

        loadRecipeCount("rejected", count -> {
            stats.rejectedRecipes = count;
            finishStatsStep(callback, stats, completed, firstError, null, totalSteps);
        }, e -> finishStatsStep(callback, stats, completed, firstError, e, totalSteps));

        loadRecipeCount("hidden", count -> {
            stats.hiddenRecipes = count;
            finishStatsStep(callback, stats, completed, firstError, null, totalSteps);
        }, e -> finishStatsStep(callback, stats, completed, firstError, e, totalSteps));

        loadCollectionCount("recipes", count -> {
            stats.totalRecipes = count;
            finishStatsStep(callback, stats, completed, firstError, null, totalSteps);
        }, e -> finishStatsStep(callback, stats, completed, firstError, e, totalSteps));

        loadCollectionCount("users", count -> {
            stats.users = count;
            finishStatsStep(callback, stats, completed, firstError, null, totalSteps);
        }, e -> finishStatsStep(callback, stats, completed, firstError, e, totalSteps));

        loadCollectionCount("favorites", count -> {
            stats.favorites = count;
            finishStatsStep(callback, stats, completed, firstError, null, totalSteps);
        }, e -> finishStatsStep(callback, stats, completed, firstError, e, totalSteps));

        loadCollectionCount("searchHistory", count -> {
            stats.searchHistory = count;
            finishStatsStep(callback, stats, completed, firstError, null, totalSteps);
        }, e -> finishStatsStep(callback, stats, completed, firstError, e, totalSteps));
    }

    private interface CountSuccess {
        void onSuccess(int count);
    }

    private interface CountError {
        void onError(Exception e);
    }

    private void loadRecipeCount(String status, CountSuccess success, CountError error) {
        db.collection("recipes").whereEqualTo("status", status).get()
                .addOnSuccessListener(snapshot -> success.onSuccess(snapshot.size()))
                .addOnFailureListener(error::onError);
    }

    private void loadCollectionCount(String collection, CountSuccess success, CountError error) {
        db.collection(collection).get()
                .addOnSuccessListener(snapshot -> success.onSuccess(snapshot.size()))
                .addOnFailureListener(error::onError);
    }

    private void finishStatsStep(StatsCallback callback, DashboardStats stats, int[] completed,
                                 Exception[] firstError, Exception error, int totalSteps) {
        if (error != null && firstError[0] == null) {
            firstError[0] = error;
        }
        completed[0]++;
        if (completed[0] == totalSteps) {
            if (firstError[0] != null) {
                Log.d(ADMIN_FLOW_TAG, "load stats partial error: " + firstError[0].getMessage());
                Log.d(TAG, "Stats partially failed: " + firstError[0].getMessage());
            }
            Log.d(ADMIN_FLOW_TAG, "load stats success pending=" + stats.pendingRecipes
                    + " approved=" + stats.approvedRecipes
                    + " rejected=" + stats.rejectedRecipes
                    + " hidden=" + stats.hiddenRecipes
                    + " users=" + stats.users);
            callback.onSuccess(stats);
        }
    }

    public void loadPendingRecipes(RecipesCallback callback) {
        db.collection("recipes")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        try {
                            recipes.add(RecipeRepository.mapRecipeDocument(document));
                        } catch (Exception e) {
                            Log.d(TAG, "Skip pending mapping error: " + e.getMessage());
                        }
                    }
                    Log.d(TAG, "Loaded pending recipes: " + recipes.size());
                    callback.onSuccess(recipes);
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Load pending error: " + e.getMessage());
                    callback.onError(e);
                });
    }

    public void loadAllRecipes(RecipesCallback callback) {
        db.collection("recipes")
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(mapRecipeDocuments(snapshot.getDocuments())))
                .addOnFailureListener(callback::onError);
    }

    public void loadRecipesByStatus(String status, RecipesCallback callback) {
        if (status == null || status.trim().isEmpty() || "all".equalsIgnoreCase(status)) {
            loadAllRecipes(callback);
            return;
        }

        db.collection("recipes")
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(mapRecipeDocuments(snapshot.getDocuments())))
                .addOnFailureListener(callback::onError);
    }

    public void searchRecipes(String keyword, String status, RecipesCallback callback) {
        loadRecipesByStatus(status, new RecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                String normalizedKeyword = hung.edu.mealmindai.utils.RecommendationEngine.normalizeVietnamese(keyword);
                if (normalizedKeyword.isEmpty()) {
                    callback.onSuccess(recipes);
                    return;
                }

                List<Recipe> filtered = new ArrayList<>();
                for (Recipe recipe : recipes) {
                    String name = hung.edu.mealmindai.utils.RecommendationEngine.normalizeVietnamese(recipe.getName());
                    String description = hung.edu.mealmindai.utils.RecommendationEngine.normalizeVietnamese(recipe.getDescription());
                    if (name.contains(normalizedKeyword) || description.contains(normalizedKeyword)) {
                        filtered.add(recipe);
                    }
                }
                callback.onSuccess(filtered);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private List<Recipe> mapRecipeDocuments(List<DocumentSnapshot> documents) {
        List<Recipe> recipes = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            try {
                recipes.add(RecipeRepository.mapRecipeDocument(document));
            } catch (Exception e) {
                Log.d(TAG, "Skip recipe mapping error: " + e.getMessage());
            }
        }
        return recipes;
    }

    public void loadRecipeById(String recipeId, RecipesCallback callback) {
        if (recipeId == null || recipeId.trim().isEmpty()) {
            callback.onError(new Exception("Không tìm thấy mã món ăn"));
            return;
        }

        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError(new Exception("Món ăn không tồn tại"));
                        return;
                    }
                    List<Recipe> result = new ArrayList<>();
                    result.add(RecipeRepository.mapRecipeDocument(document));
                    callback.onSuccess(result);
                })
                .addOnFailureListener(callback::onError);
    }

    public void approveRecipe(String recipeId, ActionCallback callback) {
        updatePendingRecipe(recipeId, null, callback);
    }

    public void rejectRecipe(String recipeId, String reason, ActionCallback callback) {
        if (reason == null || reason.trim().isEmpty()) {
            callback.onError(new Exception("Vui lòng nhập lý do từ chối"));
            return;
        }
        updatePendingRecipe(recipeId, reason.trim(), callback);
    }

    public void hideRecipe(String recipeId, ActionCallback callback) {
        updateRecipeStatusIfCurrent(recipeId, "approved", "hidden", callback);
    }

    public void restoreRecipe(String recipeId, ActionCallback callback) {
        updateRecipeStatusIfCurrent(recipeId, "hidden", "approved", callback);
    }

    private void updateRecipeStatusIfCurrent(String recipeId, String expectedStatus, String nextStatus,
                                             ActionCallback callback) {
        if (recipeId == null || recipeId.trim().isEmpty()) {
            callback.onError(new Exception("Không tìm thấy mã món ăn"));
            return;
        }

        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(document -> {
                    String status = document.getString("status");
                    if (!expectedStatus.equalsIgnoreCase(status)) {
                        callback.onError(new Exception("Chỉ có thể đổi món từ "
                                + expectedStatus + " sang " + nextStatus));
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", nextStatus);
                    updates.put("updatedAt", FieldValue.serverTimestamp());
                            db.collection("recipes").document(recipeId).update(updates)
                            .addOnSuccessListener(unused -> {
                                Log.d(ADMIN_FLOW_TAG, ("hidden".equals(nextStatus) ? "hide" : "restore")
                                        + " recipe success: " + recipeId);
                                Log.d(TAG, "Recipe status changed: " + recipeId + " -> " + nextStatus);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.d(ADMIN_FLOW_TAG, ("hidden".equals(nextStatus) ? "hide" : "restore")
                                        + " recipe error: " + e.getMessage());
                                callback.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.d(ADMIN_FLOW_TAG, ("hidden".equals(nextStatus) ? "hide" : "restore")
                            + " recipe read error: " + e.getMessage());
                    callback.onError(e);
                });
    }

    private void updatePendingRecipe(String recipeId, String rejectReason, ActionCallback callback) {
        FirebaseUser admin = auth.getCurrentUser();
        if (admin == null) {
            callback.onError(new Exception("Bạn chưa đăng nhập"));
            return;
        }
        if (recipeId == null || recipeId.trim().isEmpty()) {
            callback.onError(new Exception("Không tìm thấy mã món ăn"));
            return;
        }

        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(document -> {
                    String status = document.getString("status");
                    if (!"pending".equalsIgnoreCase(status)) {
                        callback.onError(new Exception("Món này đã được xử lý"));
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", rejectReason == null ? "approved" : "rejected");
                    updates.put("reviewedBy", admin.getUid());
                    updates.put("reviewedAt", FieldValue.serverTimestamp());
                    updates.put("updatedAt", FieldValue.serverTimestamp());
                    updates.put("rejectReason", rejectReason == null ? "" : rejectReason);

                    db.collection("recipes").document(recipeId).update(updates)
                            .addOnSuccessListener(unused -> {
                                Log.d(ADMIN_FLOW_TAG, rejectReason == null
                                        ? "approve recipe success: " + recipeId
                                        : "reject recipe success: " + recipeId);
                                Log.d(TAG, rejectReason == null
                                        ? "Approve success: " + recipeId
                                        : "Reject success: " + recipeId);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.d(ADMIN_FLOW_TAG, rejectReason == null
                                        ? "approve recipe error: " + e.getMessage()
                                        : "reject recipe error: " + e.getMessage());
                                Log.d(TAG, "Review update error: " + e.getMessage());
                                callback.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.d(ADMIN_FLOW_TAG, rejectReason == null
                            ? "approve recipe read error: " + e.getMessage()
                            : "reject recipe read error: " + e.getMessage());
                    Log.d(TAG, "Review read error: " + e.getMessage());
                    callback.onError(e);
                });
    }
}
