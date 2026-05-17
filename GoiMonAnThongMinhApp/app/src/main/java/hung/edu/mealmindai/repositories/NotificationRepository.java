package hung.edu.mealmindai.repositories;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hung.edu.mealmindai.models.AppNotification;

public class NotificationRepository {
    private static final String TAG = "NotificationRepository";
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface NotificationCallback {
        void onSuccess(List<AppNotification> notifications);
        void onError(Exception e);
        void onLoginRequired();
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public void loadCurrentUserNotifications(NotificationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onLoginRequired();
            return;
        }

        db.collection("notifications")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AppNotification> notifications = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        AppNotification notification = document.toObject(AppNotification.class);
                        if (notification != null) {
                            notification.setNotificationId(document.getId());
                            notifications.add(notification);
                        }
                    }
                    Collections.sort(notifications, (first, second) -> {
                        long firstTime = first.getCreatedAt() != null ? first.getCreatedAt().toDate().getTime() : 0L;
                        long secondTime = second.getCreatedAt() != null ? second.getCreatedAt().toDate().getTime() : 0L;
                        return Long.compare(secondTime, firstTime);
                    });
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(callback::onError);
    }

    public void markAsRead(String notificationId) {
        if (notificationId == null || notificationId.trim().isEmpty()) {
            return;
        }
        db.collection("notifications").document(notificationId)
                .update("read", true)
                .addOnFailureListener(e -> Log.d(TAG, "mark read error: " + e.getMessage()));
    }

    public void createDailySuggestionNotificationIfNeeded(ActionCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(new Exception("Bạn chưa đăng nhập"));
            return;
        }

        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        String documentId = user.getUid() + "_daily_" + today;
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUid());
        data.put("title", "Gợi ý món hôm nay");
        data.put("message", "Mở MealMind AI để chọn món hợp khẩu vị, ngân sách và thời gian nấu của bạn.");
        data.put("type", "daily_suggestion");
        data.put("recipeId", "");
        data.put("read", false);
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("notifications").document(documentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onSuccess();
                        return;
                    }
                    db.collection("notifications").document(documentId).set(data)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public static void createRecipeReviewNotification(String userId, String recipeId, String recipeName,
                                                      boolean approved, String rejectReason) {
        if (userId == null || userId.trim().isEmpty() || "system".equalsIgnoreCase(userId)) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("title", approved ? "Công thức đã được duyệt" : "Công thức bị từ chối");
        data.put("message", approved
                ? "Món " + safeRecipeName(recipeName) + " đã được duyệt và hiển thị cho mọi người."
                : "Món " + safeRecipeName(recipeName) + " bị từ chối. Lý do: " + safeReason(rejectReason));
        data.put("type", approved ? "recipe_approved" : "recipe_rejected");
        data.put("recipeId", recipeId == null ? "" : recipeId);
        data.put("read", false);
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("notifications")
                .add(data)
                .addOnSuccessListener(documentReference ->
                        Log.d(TAG, "review notification created: " + documentReference.getId()))
                .addOnFailureListener(e ->
                        Log.d(TAG, "review notification error: " + e.getMessage()));
    }

    private static String safeRecipeName(String recipeName) {
        return recipeName == null || recipeName.trim().isEmpty() ? "của bạn" : recipeName;
    }

    private static String safeReason(String reason) {
        return reason == null || reason.trim().isEmpty() ? "Chưa có lý do cụ thể" : reason;
    }
}
