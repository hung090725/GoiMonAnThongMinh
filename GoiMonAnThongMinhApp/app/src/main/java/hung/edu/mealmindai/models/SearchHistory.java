package hung.edu.mealmindai.models;

import com.google.firebase.Timestamp;
import java.util.Map;

/**
 * User search record stored in the search_histories collection.
 */
public class SearchHistory {
    // Firestore document id or custom search history id.
    private String searchHistoryId;
    // User and search input.
    private String userId;
    private String keyword;
    // Optional Firestore map for filters such as category or calories.
    private Map<String, Object> filters;
    private Integer resultCount;
    private Timestamp searchedAt;

    /**
     * Required empty constructor for Firebase.
     */
    public SearchHistory() {
    }

    public SearchHistory(String searchHistoryId, String userId, String keyword,
                         Map<String, Object> filters, Integer resultCount, Timestamp searchedAt) {
        this.searchHistoryId = searchHistoryId;
        this.userId = userId;
        this.keyword = keyword;
        this.filters = filters;
        this.resultCount = resultCount;
        this.searchedAt = searchedAt;
    }

    public String getSearchHistoryId() {
        return searchHistoryId;
    }

    public void setSearchHistoryId(String searchHistoryId) {
        this.searchHistoryId = searchHistoryId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public Timestamp getSearchedAt() {
        return searchedAt;
    }

    public void setSearchedAt(Timestamp searchedAt) {
        this.searchedAt = searchedAt;
    }
}
