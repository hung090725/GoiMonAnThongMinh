package hung.edu.mealmindai.repositories;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import hung.edu.mealmindai.models.User;

/**
 * Repository xử lý thông tin người dùng trong Firestore.
 * Sử dụng mapping thủ công để tránh lỗi ép kiểu (Long/Double) từ Firestore.
 */
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
                            User user = mapSnapshotToUser(documentSnapshot);
                            callback.onSuccess(user);
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi map user: " + e.getMessage());
                            callback.onError(e);
                        }
                    } else {
                        callback.onError(new Exception("Hồ sơ không tồn tại"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private User mapSnapshotToUser(DocumentSnapshot doc) {
        User user = new User();
        user.setUid(doc.getId());
        user.setFullName(doc.getString("fullName"));
        user.setEmail(doc.getString("email"));
        user.setHealthGoal(doc.getString("healthGoal"));
        
        // Xử lý an toàn các kiểu số (Firestore có thể trả về Long hoặc Double)
        user.setHeight(toDouble(doc.get("height")));
        user.setWeight(toDouble(doc.get("weight")));
        user.setMonthlyFoodBudget(toDouble(doc.get("monthlyFoodBudget")));
        user.setMealBudget(toDouble(doc.get("mealBudget")));
        user.setAvailableTime(toInteger(doc.get("availableTime")));
        
        user.setUpdatedAt(doc.get("updatedAt"));
        return user;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
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
