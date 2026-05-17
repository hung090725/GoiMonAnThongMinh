package hung.edu.mealmindai.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.AiSuggestionHistory;

public class AiSuggestionHistoryAdapter extends RecyclerView.Adapter<AiSuggestionHistoryAdapter.HistoryViewHolder> {
    public interface OnHistoryClickListener {
        void onHistoryClick(AiSuggestionHistory history);
        void onHistoryDelete(AiSuggestionHistory history);
    }

    private final List<AiSuggestionHistory> histories = new ArrayList<>();
    private final OnHistoryClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.US);

    public AiSuggestionHistoryAdapter(OnHistoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(histories.get(position));
    }

    @Override
    public int getItemCount() {
        return histories.size();
    }

    public void submitList(List<AiSuggestionHistory> newHistories) {
        histories.clear();
        if (newHistories != null) {
            histories.addAll(newHistories);
        }
        notifyDataSetChanged();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView textInput;
        private final TextView textResponse;
        private final TextView textTime;
        private final TextView textOpenRecipe;
        private final TextView textDelete;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textInput = itemView.findViewById(R.id.textHistoryInput);
            textResponse = itemView.findViewById(R.id.textHistoryResponse);
            textTime = itemView.findViewById(R.id.textHistoryTime);
            textOpenRecipe = itemView.findViewById(R.id.textOpenHistoryRecipe);
            textDelete = itemView.findViewById(R.id.textDeleteHistory);
        }

        void bind(AiSuggestionHistory history) {
            textInput.setText(emptyToDefault(history.getInputText(), "Câu hỏi chưa có nội dung"));
            textResponse.setText(shorten(emptyToDefault(history.getResponseText(), "Chưa có phản hồi")));
            textTime.setText(history.getCreatedAt() != null
                    ? dateFormat.format(history.getCreatedAt().toDate())
                    : "Vừa xong");
            textOpenRecipe.setVisibility(hasRecipe(history) ? View.VISIBLE : View.GONE);
            itemView.setOnClickListener(v -> listener.onHistoryClick(history));
            textOpenRecipe.setOnClickListener(v -> listener.onHistoryClick(history));
            textDelete.setOnClickListener(v -> listener.onHistoryDelete(history));
        }

        private boolean hasRecipe(AiSuggestionHistory history) {
            return history.getSuggestedRecipeIds() != null && !history.getSuggestedRecipeIds().isEmpty()
                    && !TextUtils.isEmpty(history.getSuggestedRecipeIds().get(0));
        }

        private String emptyToDefault(String value, String fallback) {
            return TextUtils.isEmpty(value) ? fallback : value;
        }

        private String shorten(String value) {
            return value.length() > 140 ? value.substring(0, 140) + "..." : value;
        }
    }
}
