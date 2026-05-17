package hung.edu.mealmindai.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hung.edu.mealmindai.models.AiSuggestionHistory;

public class AiSuggestionHistoryRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface HistoryCallback {
        void onSuccess(List<AiSuggestionHistory> histories);
        void onError(Exception e);
        void onLoginRequired();
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public void loadCurrentUserHistory(HistoryCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onLoginRequired();
            return;
        }

        db.collection("aiSuggestionHistory")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AiSuggestionHistory> histories = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        AiSuggestionHistory history = document.toObject(AiSuggestionHistory.class);
                        if (history != null) {
                            history.setHistoryId(document.getId());
                            histories.add(history);
                        }
                    }
                    Collections.sort(histories, (first, second) -> {
                        long firstTime = first.getCreatedAt() != null ? first.getCreatedAt().toDate().getTime() : 0L;
                        long secondTime = second.getCreatedAt() != null ? second.getCreatedAt().toDate().getTime() : 0L;
                        return Long.compare(secondTime, firstTime);
                    });
                    callback.onSuccess(histories);
                })
                .addOnFailureListener(callback::onError);
    }

    public void deleteHistory(String historyId, ActionCallback callback) {
        if (historyId == null || historyId.trim().isEmpty()) {
            callback.onError(new Exception("Không tìm thấy lịch sử"));
            return;
        }

        db.collection("aiSuggestionHistory").document(historyId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
}
