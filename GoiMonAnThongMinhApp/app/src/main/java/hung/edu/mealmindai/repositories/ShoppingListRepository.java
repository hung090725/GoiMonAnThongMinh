package hung.edu.mealmindai.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingListRepository {
    private static final String COLLECTION_SHOPPING_LISTS = "shoppingLists";

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception e);
        void onLoginRequired();
    }

    public interface CheckedItemsCallback {
        void onSuccess(List<String> checkedItems);
        void onError(Exception e);
        void onLoginRequired();
    }

    public void loadCheckedItems(String recipeId, CheckedItemsCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onLoginRequired();
            return;
        }

        String documentId = buildDocumentId(currentUser.getUid(), recipeId);
        db.collection(COLLECTION_SHOPPING_LISTS)
                .document(documentId)
                .get()
                .addOnSuccessListener(document -> callback.onSuccess(readStringList(document, "checkedItems")))
                .addOnFailureListener(callback::onError);
    }

    public void saveShoppingList(String recipeId, String recipeName,
                                 List<String> items, List<String> checkedItems,
                                 ActionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onLoginRequired();
            return;
        }

        if (recipeId == null || recipeId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Thiếu mã món ăn"));
            return;
        }

        String documentId = buildDocumentId(currentUser.getUid(), recipeId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", currentUser.getUid());
        data.put("recipeId", recipeId);
        data.put("recipeName", recipeName == null ? "" : recipeName);
        data.put("items", items);
        data.put("checkedItems", checkedItems);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_SHOPPING_LISTS)
                .document(documentId)
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    private String buildDocumentId(String userId, String recipeId) {
        return safeId(userId) + "_" + safeId(recipeId);
    }

    private List<String> readStringList(DocumentSnapshot document, String field) {
        List<String> result = new ArrayList<>();
        if (document == null || !document.exists()) {
            return result;
        }

        Object value = document.get(field);
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
        }
        return result;
    }

    private String safeId(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
