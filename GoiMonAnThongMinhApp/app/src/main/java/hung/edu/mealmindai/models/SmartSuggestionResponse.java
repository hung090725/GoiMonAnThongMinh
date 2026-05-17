package hung.edu.mealmindai.models;

import java.util.List;

/**
 * Rule-based response returned by SmartMealAssistant.
 */
public class SmartSuggestionResponse {
    private String responseText;
    private List<RecipeScoreResult> results;
    private boolean hasResult;

    public SmartSuggestionResponse() {
    }

    public SmartSuggestionResponse(String responseText, List<RecipeScoreResult> results, boolean hasResult) {
        this.responseText = responseText;
        this.results = results;
        this.hasResult = hasResult;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public List<RecipeScoreResult> getResults() {
        return results;
    }

    public void setResults(List<RecipeScoreResult> results) {
        this.results = results;
    }

    public boolean isHasResult() {
        return hasResult;
    }

    public void setHasResult(boolean hasResult) {
        this.hasResult = hasResult;
    }
}
