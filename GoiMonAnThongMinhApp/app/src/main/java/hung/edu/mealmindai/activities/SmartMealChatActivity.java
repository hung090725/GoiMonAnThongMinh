package hung.edu.mealmindai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.adapters.ChatMessageAdapter;
import hung.edu.mealmindai.models.ChatMessage;
import hung.edu.mealmindai.models.SmartSuggestionResponse;
import hung.edu.mealmindai.repositories.SmartSuggestionRepository;

/**
 * Rule-based chat assistant for meal suggestions.
 */
public class SmartMealChatActivity extends AppCompatActivity {
    private static final String SENDER_USER = "user";
    private static final String SENDER_AI = "ai";

    private ChatMessageAdapter chatAdapter;
    private SmartSuggestionRepository repository;
    private RecyclerView recyclerChatMessages;
    private EditText editChatInput;
    private MaterialButton buttonSendChat;
    private ProgressBar progressChat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_meal_chat);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            openLoginAndClearTask();
            return;
        }

        repository = new SmartSuggestionRepository();
        initViews();
        setupChatList();
        setupActions();
        loadChatHistory();
    }

    private void initViews() {
        ImageButton buttonBack = findViewById(R.id.buttonChatBack);
        recyclerChatMessages = findViewById(R.id.recyclerChatMessages);
        editChatInput = findViewById(R.id.editChatInput);
        buttonSendChat = findViewById(R.id.buttonSendChat);
        progressChat = findViewById(R.id.progressChat);

        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupChatList() {
        chatAdapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerChatMessages.setLayoutManager(layoutManager);
        recyclerChatMessages.setAdapter(chatAdapter);
    }

    private void setupActions() {
        buttonSendChat.setOnClickListener(v -> sendCurrentMessage());
        editChatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentMessage();
                return true;
            }
            return false;
        });
    }

    private void addWelcomeMessage() {
        chatAdapter.addMessage(new ChatMessage(
                createMessageId(),
                "Bạn cứ nhập nguyên liệu, ngân sách hoặc thời gian nấu. Mình sẽ gợi ý món phù hợp nhất từ dữ liệu đã duyệt.",
                SENDER_AI,
                System.currentTimeMillis()
        ));
        scrollToBottom();
    }

    private void loadChatHistory() {
        setLoading(true);
        repository.loadRecentChatHistory(new SmartSuggestionRepository.HistoryCallback() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (messages == null || messages.isEmpty()) {
                        addWelcomeMessage();
                    } else {
                        chatAdapter.submitMessages(messages);
                        scrollToBottom();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    addWelcomeMessage();
                });
            }

            @Override
            public void onLoginRequired() {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(SmartMealChatActivity.this,
                            "Vui lòng đăng nhập",
                            Toast.LENGTH_SHORT).show();
                    openLoginAndClearTask();
                });
            }
        });
    }

    private void sendCurrentMessage() {
        String input = editChatInput.getText() != null ? editChatInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(input)) {
            Toast.makeText(this, "Vui lòng nhập nhu cầu món ăn", Toast.LENGTH_SHORT).show();
            return;
        }

        chatAdapter.addMessage(new ChatMessage(createMessageId(), input, SENDER_USER, System.currentTimeMillis()));
        editChatInput.setText("");
        chatAdapter.addMessage(new ChatMessage(createMessageId(),
                "MealMind AI đang phân tích...",
                SENDER_AI,
                System.currentTimeMillis()));
        setLoading(true);
        scrollToBottom();

        repository.loadDataAndSuggest(input, new SmartSuggestionRepository.SuggestionCallback() {
            @Override
            public void onSuccess(SmartSuggestionResponse response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    chatAdapter.removeLastLoadingMessageIfNeeded();
                    chatAdapter.addMessage(new ChatMessage(
                            createMessageId(),
                            response != null ? response.getResponseText() : "Hiện mình chưa có câu trả lời phù hợp.",
                            SENDER_AI,
                            System.currentTimeMillis()
                    ));
                    scrollToBottom();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    chatAdapter.removeLastLoadingMessageIfNeeded();
                    chatAdapter.addMessage(new ChatMessage(
                            createMessageId(),
                            "Không thể tải dữ liệu gợi ý. Bạn thử lại sau nhé.",
                            SENDER_AI,
                            System.currentTimeMillis()
                    ));
                    Toast.makeText(SmartMealChatActivity.this,
                            "Không thể tải dữ liệu gợi ý",
                            Toast.LENGTH_SHORT).show();
                    scrollToBottom();
                });
            }

            @Override
            public void onLoginRequired() {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(SmartMealChatActivity.this,
                            "Vui lòng đăng nhập",
                            Toast.LENGTH_SHORT).show();
                    openLoginAndClearTask();
                });
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressChat.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        buttonSendChat.setEnabled(!isLoading);
        editChatInput.setEnabled(!isLoading);
    }

    private void scrollToBottom() {
        recyclerChatMessages.post(() -> {
            int lastIndex = chatAdapter.getItemCount() - 1;
            if (lastIndex >= 0) {
                recyclerChatMessages.smoothScrollToPosition(lastIndex);
            }
        });
    }

    private String createMessageId() {
        return "msg_" + System.currentTimeMillis();
    }

    private void openLoginAndClearTask() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
