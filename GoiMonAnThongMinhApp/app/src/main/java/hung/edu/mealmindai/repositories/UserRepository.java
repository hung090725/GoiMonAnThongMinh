package hung.edu.mealmindai.repositories;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import hung.edu.mealmindai.models.User;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String COLLECTION_USERS = "users";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onError(Exception e);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public void getCurrentUserProfile(UserCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("Chưa đăng nhập"));
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            User user = mapUserDocument(documentSnapshot);
                            callback.onSuccess(user);
                        } catch (Exception e) {
                            Log.e(TAG, "Error deserializing user: " + e.getMessage());
                            callback.onError(e);
                        }
                    } else {
                        callback.onError(new Exception("Hồ sơ không tồn tại"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private User mapUserDocument(DocumentSnapshot document) {
        User user = new User();
        user.setUid(firstNonEmpty(document.getString("uid"), document.getId()));
        user.setUserId(document.getString("userId"));
        user.setFullName(document.getString("fullName"));
        user.setEmail(document.getString("email"));
        user.setAvatarUrl(document.getString("avatarUrl"));
        user.setPhotoUrl(document.getString("photoUrl"));
        user.setHeight(toDouble(document.get("height")));
        user.setWeight(toDouble(document.get("weight")));
        user.setHealthGoal(document.getString("healthGoal"));
        user.setMonthlyFoodBudget(toDouble(document.get("monthlyFoodBudget")));
        user.setMealBudget(toDouble(document.get("mealBudget")));
        user.setAvailableTime(toInteger(document.get("availableTime")));
        user.setRole(document.getString("role"));
        user.setAge(toInteger(document.get("age")));
        user.setGender(document.getString("gender"));
        user.setHeightCm(toDouble(document.get("heightCm")));
        user.setWeightKg(toDouble(document.get("weightKg")));
        user.setGoal(document.getString("goal"));
        user.setDietaryPreference(document.getString("dietaryPreference"));
        user.setAllergies(toStringList(document.get("allergies")));
        user.setFavoriteCategoryIds(toStringList(document.get("favoriteCategoryIds")));
        user.setCreatedAt(document.getTimestamp("createdAt"));
        user.setUpdatedAt(document.getTimestamp("updatedAt"));
        return user;
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }

    private Integer toInteger(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private Double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : null;
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

    public void updateHealthProfile(Double height, Double weight, String healthGoal, 
                                   Double monthlyBudget, Double mealBudget, Integer availableTime,
                                   ActionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("Chưa đăng nhập"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("height", height);
        updates.put("weight", weight);
        updates.put("healthGoal", healthGoal);
        updates.put("monthlyFoodBudget", monthlyBudget);
        updates.put("mealBudget", mealBudget);
        updates.put("availableTime", availableTime);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
}
