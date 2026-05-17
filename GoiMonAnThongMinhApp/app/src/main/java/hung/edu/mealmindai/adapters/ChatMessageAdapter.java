package hung.edu.mealmindai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hung.edu.mealmindai.R;
import hung.edu.mealmindai.models.ChatMessage;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;
    private final List<ChatMessage> messages = new ArrayList<>();

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return "user".equalsIgnoreCase(message.getSenderType()) ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = viewType == VIEW_TYPE_USER ? R.layout.item_chat_user : R.layout.item_chat_ai;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void submitMessages(List<ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    public void removeLastLoadingMessageIfNeeded() {
        if (messages.isEmpty()) {
            return;
        }
        ChatMessage lastMessage = messages.get(messages.size() - 1);
        if ("ai".equalsIgnoreCase(lastMessage.getSenderType())
                && lastMessage.getText() != null
                && lastMessage.getText().contains("đang phân tích")) {
            int index = messages.size() - 1;
            messages.remove(index);
            notifyItemRemoved(index);
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textChatMessage);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getText());
        }
    }
}
