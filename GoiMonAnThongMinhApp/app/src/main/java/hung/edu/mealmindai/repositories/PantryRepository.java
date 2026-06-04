package hung.edu.mealmindai.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PantryRepository {
    private static final String COLLECTION_PANTRY = "pantry";

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface PantryCallback {
        void onSuccess(List<String> ingredients);
        void onError(Exception e);
        void onLoginRequired();
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception e);
        void onLoginRequired();
    }

    public void loadCurrentUserPantry(PantryCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onLoginRequired();
            return;
        }

        db.collection(COLLECTION_PANTRY)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> callback.onSuccess(toStringList(document.get("ingredients"))))
                .addOnFailureListener(callback::onError);
    }

    public void saveCurrentUserPantry(List<String> ingredients, ActionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onLoginRequired();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", currentUser.getUid());
        data.put("ingredients", ingredients != null ? ingredients : new ArrayList<String>());
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_PANTRY)
                .document(currentUser.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    private List<String> toStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item != null) {
                    String text = item.toString().trim();
                    if (!text.isEmpty() && !containsIgnoreCase(result, text)) {
                        result.add(text);
                    }
                }
            }
        }
        return result;
    }

    private boolean containsIgnoreCase(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }
}
