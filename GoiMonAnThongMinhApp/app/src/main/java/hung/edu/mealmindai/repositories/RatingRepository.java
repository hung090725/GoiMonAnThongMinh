package hung.edu.mealmindai.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class RatingRepository {
    private static final String COLLECTION_RATINGS = "ratings";

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface RatingCallback {
        void onSuccess(float userRating, double averageRating);
        void onError(Exception e);
        void onLoginRequired();
    }

    public interface ActionCallback {
        void onSuccess(double averageRating);
        void onError(Exception e);
        void onLoginRequired();
    }

    public void loadRatingInfo(String recipeId, RatingCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onLoginRequired();
            return;
        }

        String ratingId = buildRatingId(currentUser.getUid(), recipeId);
        db.collection(COLLECTION_RATINGS)
                .document(ratingId)
                .get()
                .addOnSuccessListener(document -> {
                    float userRating = toFloat(document.get("rating"));
                    loadAverageRating(recipeId, new AverageCallback() {
                        @Override
                        public void onSuccess(double averageRating) {
                            callback.onSuccess(userRating, averageRating);
                        }

                        @Override
                        public void onError(Exception e) {
                            callback.onError(e);
                        }
                    });
                })
                .addOnFailureListener(callback::onError);
    }

    public void saveRating(String recipeId, float rating, ActionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onLoginRequired();
            return;
        }

        if (recipeId == null || recipeId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Thiếu mã món ăn"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", currentUser.getUid());
        data.put("recipeId", recipeId);
        data.put("rating", rating);
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_RATINGS)
                .document(buildRatingId(currentUser.getUid(), recipeId))
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> loadAverageRating(recipeId, new AverageCallback() {
                    @Override
                    public void onSuccess(double averageRating) {
                        callback.onSuccess(averageRating);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                }))
                .addOnFailureListener(callback::onError);
    }

    private void loadAverageRating(String recipeId, AverageCallback callback) {
        db.collection(COLLECTION_RATINGS)
                .whereEqualTo("recipeId", recipeId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    double total = 0;
                    int count = 0;
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        float rating = toFloat(document.get("rating"));
                        if (rating > 0) {
                            total += rating;
                            count++;
                        }
                    }
                    callback.onSuccess(count == 0 ? 0 : total / count);
                })
                .addOnFailureListener(callback::onError);
    }

    private String buildRatingId(String userId, String recipeId) {
        return safeId(userId) + "_" + safeId(recipeId);
    }

    private String safeId(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private float toFloat(Object value) {
        return value instanceof Number ? ((Number) value).floatValue() : 0f;
    }

    private interface AverageCallback {
        void onSuccess(double averageRating);
        void onError(Exception e);
    }
}
