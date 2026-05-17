package hung.edu.mealmindai.models;

import com.google.firebase.Timestamp;

/**
 * User favorite recipe stored in the favorites collection.
 */
public class Favorite {
    // Firestore document id or custom favorite id.
    private String favoriteId;
    // Links a user to a recipe.
    private String userId;
    private String recipeId;
    private Timestamp createdAt;

    /**
     * Required empty constructor for Firebase.
     */
    public Favorite() {
    }

    public Favorite(String favoriteId, String userId, String recipeId, Timestamp createdAt) {
        this.favoriteId = favoriteId;
        this.userId = userId;
        this.recipeId = recipeId;
        this.createdAt = createdAt;
    }

    public String getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(String favoriteId) {
        this.favoriteId = favoriteId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
