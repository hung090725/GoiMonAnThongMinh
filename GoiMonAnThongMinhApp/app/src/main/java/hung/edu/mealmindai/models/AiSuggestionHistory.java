package hung.edu.mealmindai.models;

import com.google.firebase.Timestamp;

import java.util.ArrayList;

/**
 * Saved history of a MealMind AI suggestion conversation.
 */
public class AiSuggestionHistory {
    private String historyId;
    private String userId;
    private String inputText;
    private String responseText;
    private ArrayList<String> suggestedRecipeIds;
    private Timestamp createdAt;

    public AiSuggestionHistory() {
    }

    public AiSuggestionHistory(String historyId, String userId, String inputText, String responseText,
                               ArrayList<String> suggestedRecipeIds, Timestamp createdAt) {
        this.historyId = historyId;
        this.userId = userId;
        this.inputText = inputText;
        this.responseText = responseText;
        this.suggestedRecipeIds = suggestedRecipeIds;
        this.createdAt = createdAt;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public ArrayList<String> getSuggestedRecipeIds() {
        return suggestedRecipeIds;
    }

    public void setSuggestedRecipeIds(ArrayList<String> suggestedRecipeIds) {
        this.suggestedRecipeIds = suggestedRecipeIds;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
