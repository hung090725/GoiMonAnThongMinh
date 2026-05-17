package hung.edu.mealmindai.adapters;

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
import hung.edu.mealmindai.models.AppNotification;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
    }

    private final List<AppNotification> notifications = new ArrayList<>();
    private final OnNotificationClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.US);

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void submitList(List<AppNotification> newNotifications) {
        notifications.clear();
        if (newNotifications != null) {
            notifications.addAll(newNotifications);
        }
        notifyDataSetChanged();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDot;
        private final TextView textTitle;
        private final TextView textMessage;
        private final TextView textType;
        private final TextView textTime;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            textDot = itemView.findViewById(R.id.textNotificationDot);
            textTitle = itemView.findViewById(R.id.textNotificationTitle);
            textMessage = itemView.findViewById(R.id.textNotificationMessage);
            textType = itemView.findViewById(R.id.textNotificationType);
            textTime = itemView.findViewById(R.id.textNotificationTime);
        }

        void bind(AppNotification notification) {
            textTitle.setText(notification.getTitle());
            textMessage.setText(notification.getMessage());
            textType.setText(formatType(notification.getType()));
            textDot.setVisibility(notification.isRead() ? View.INVISIBLE : View.VISIBLE);
            textTime.setText(notification.getCreatedAt() != null
                    ? dateFormat.format(notification.getCreatedAt().toDate())
                    : "Vừa xong");
            itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
        }

        private String formatType(String type) {
            if ("recipe_approved".equalsIgnoreCase(type)) {
                return "Đã duyệt";
            }
            if ("recipe_rejected".equalsIgnoreCase(type)) {
                return "Từ chối";
            }
            return "Gợi ý";
        }
    }
}
