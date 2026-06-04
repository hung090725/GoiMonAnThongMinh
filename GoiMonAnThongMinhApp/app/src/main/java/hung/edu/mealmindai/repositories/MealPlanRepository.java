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

import hung.edu.mealmindai.models.MealPlan;

public class MealPlanRepository {
    private static final String COLLECTION_MEAL_PLANS = "mealPlans";

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception e);
        void onLoginRequired();
    }

    public interface MealPlansCallback {
        void onSuccess(List<MealPlan> mealPlans);
        void onError(Exception e);
        void onLoginRequired();
    }

    public void loadPlansForDate(String mealDate, MealPlansCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onLoginRequired();
            return;
        }

        db.collection(COLLECTION_MEAL_PLANS)
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<MealPlan> plans = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        MealPlan plan = mapMealPlan(document);
                        if (mealDate != null && mealDate.equals(plan.getMealDate())) {
                            plans.add(plan);
                        }
                    }
                    callback.onSuccess(plans);
                })
                .addOnFailureListener(callback::onError);
    }

    public void addRecipeToTodayPlan(String recipeId, String recipeName,
                                     String mealDate, String mealType,
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

        String uid = currentUser.getUid();
        String planId = buildPlanId(uid, recipeId, mealDate, mealType);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("recipeId", recipeId);
        data.put("recipeName", recipeName == null ? "" : recipeName);
        data.put("mealDate", mealDate);
        data.put("mealType", mealType);
        data.put("completed", false);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_MEAL_PLANS)
                .document(planId)
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void updateCompleted(String mealPlanId, boolean completed, ActionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onLoginRequired();
            return;
        }

        if (mealPlanId == null || mealPlanId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Thiếu mã kế hoạch"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("completed", completed);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        db.collection(COLLECTION_MEAL_PLANS)
                .document(mealPlanId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    private MealPlan mapMealPlan(DocumentSnapshot document) {
        MealPlan plan = new MealPlan();
        plan.setMealPlanId(document.getId());
        plan.setUserId(document.getString("userId"));
        plan.setRecipeId(document.getString("recipeId"));
        plan.setRecipeName(document.getString("recipeName"));
        plan.setMealDate(document.getString("mealDate"));
        plan.setMealType(document.getString("mealType"));
        Boolean completed = document.getBoolean("completed");
        plan.setCompleted(completed != null && completed);
        return plan;
    }

    private String buildPlanId(String userId, String recipeId, String mealDate, String mealType) {
        return safeId(userId) + "_" + safeId(recipeId) + "_" + safeId(mealDate) + "_" + safeId(mealType);
    }

    private String safeId(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
