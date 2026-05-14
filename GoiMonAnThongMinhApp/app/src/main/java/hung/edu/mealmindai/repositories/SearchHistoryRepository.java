package hung.edu.mealmindai.repositories;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository xử lý lưu trữ lịch sử tìm kiếm.
 */
public class SearchHistoryRepository {
    private static final String TAG = "SearchHistoryRepository";
    private static final String COLLECTION_HISTORY = "searchHistory";
    private final FirebaseFirestore db;

    public SearchHistoryRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Lưu lịch sử tìm kiếm vào Firestore.
     */
    public void saveSearchHistory(String inputText, List<String> ingredients) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> history = new HashMap<>();
        history.put("userId", currentUser.getUid());
        history.put("inputText", inputText);
        history.put("ingredients", ingredients);
        history.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_HISTORY)
                .add(history)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Đã lưu lịch sử tìm kiếm: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi lưu lịch sử: " + e.getMessage()));
    }
}
