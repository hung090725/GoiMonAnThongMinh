package hung.edu.mealmindai.repositories;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hung.edu.mealmindai.models.Category;

public class CategoryRepository {
    private static final String ADMIN_FLOW_TAG = "AdminFlow";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface CategoryCallback {
        void onSuccess(List<Category> categories);
        void onError(Exception e);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public void loadCategories(CategoryCallback callback) {
        db.collection("categories")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Category> categories = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        categories.add(mapCategory(document));
                    }
                    callback.onSuccess(categories);
                })
                .addOnFailureListener(callback::onError);
    }

    public void addCategory(String name, String description, String iconUrl, ActionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", description);
        data.put("iconUrl", iconUrl);
        data.put("status", "active");
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("categories").add(data)
                .addOnSuccessListener(documentReference -> {
                    Log.d(ADMIN_FLOW_TAG, "add category success: " + documentReference.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.d(ADMIN_FLOW_TAG, "add category error: " + e.getMessage());
                    callback.onError(e);
                });
    }

    public void updateCategory(String categoryId, String name, String description, String iconUrl,
                               ActionCallback callback) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            callback.onError(new Exception("Không tìm thấy mã danh mục"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", description);
        data.put("iconUrl", iconUrl);
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("categories").document(categoryId).update(data)
                .addOnSuccessListener(unused -> {
                    Log.d(ADMIN_FLOW_TAG, "update category success: " + categoryId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.d(ADMIN_FLOW_TAG, "update category error: " + e.getMessage());
                    callback.onError(e);
                });
    }

    public void hideCategory(String categoryId, ActionCallback callback) {
        updateCategoryStatus(categoryId, "hidden", callback);
    }

    public void restoreCategory(String categoryId, ActionCallback callback) {
        updateCategoryStatus(categoryId, "active", callback);
    }

    private void updateCategoryStatus(String categoryId, String status, ActionCallback callback) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            callback.onError(new Exception("Không tìm thấy mã danh mục"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("categories").document(categoryId).update(data)
                .addOnSuccessListener(unused -> {
                    Log.d(ADMIN_FLOW_TAG, ("hidden".equals(status) ? "hide" : "restore")
                            + " category success: " + categoryId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.d(ADMIN_FLOW_TAG, ("hidden".equals(status) ? "hide" : "restore")
                            + " category error: " + e.getMessage());
                    callback.onError(e);
                });
    }

    private Category mapCategory(DocumentSnapshot document) {
        Category category = new Category();
        category.setCategoryId(document.getId());
        category.setName(document.getString("name"));
        category.setDescription(document.getString("description"));
        category.setIconUrl(document.getString("iconUrl"));
        category.setImageUrl(document.getString("imageUrl"));
        category.setStatus(document.getString("status"));
        category.setCreatedAt(document.getTimestamp("createdAt"));
        category.setUpdatedAt(document.getTimestamp("updatedAt"));
        return category;
    }
}
