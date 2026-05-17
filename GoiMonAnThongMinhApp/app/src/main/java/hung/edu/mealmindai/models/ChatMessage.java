package hung.edu.mealmindai.models;

/**
 * Message item used by the MealMind AI chat screen.
 */
public class ChatMessage {
    private String messageId;
    private String text;
    private String senderType;
    private long createdAtLocal;

    public ChatMessage() {
    }

    public ChatMessage(String messageId, String text, String senderType, long createdAtLocal) {
        this.messageId = messageId;
        this.text = text;
        this.senderType = senderType;
        this.createdAtLocal = createdAtLocal;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public long getCreatedAtLocal() {
        return createdAtLocal;
    }

    public void setCreatedAtLocal(long createdAtLocal) {
        this.createdAtLocal = createdAtLocal;
    }
}
