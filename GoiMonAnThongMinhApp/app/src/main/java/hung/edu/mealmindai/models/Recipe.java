package hung.edu.mealmindai.models;

import com.google.firebase.Timestamp;
import java.util.List;

/**
 * Recipe data stored in the recipes collection.
 */
public class Recipe {
    // Firestore document id or custom recipe id.
    private String recipeId;
    // Main recipe content shown to users.
    private String name;
    private String title;
    private String description;
    private String imageUrl;
    private String categoryId;
    private List<String> ingredients;
    private List<String> steps;
    // Nutrition and cooking details.
    private Integer calories;
    private Double estimatedCost;
    private Integer cookingTime;
    private Double protein;
    private Double carbs;
    private Double fat;
    private Integer cookingTimeMinutes;
    private Integer servingSize;
    private String difficulty;
    private List<String> tags;
    // Ownership and metadata.
    private String authorId;
    private String authorName;
    private String status;
    private Integer likeCount;
    private String createdBy;
    private Boolean aiGenerated;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp reviewedAt;
    private String reviewedBy;
    private String rejectReason;

    /**
     * Required empty constructor for Firebase.
     */
    public Recipe() {
    }

    public Recipe(String recipeId, String title, String description, String imageUrl,
                  String categoryId, List<String> ingredients, List<String> steps,
                  Integer calories, Double protein, Double carbs, Double fat,
                  Integer cookingTimeMinutes, Integer servingSize, String difficulty,
                  List<String> tags, String createdBy, Boolean aiGenerated,
                  Timestamp createdAt, Timestamp updatedAt) {
        this.recipeId = recipeId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.ingredients = ingredients;
        this.steps = steps;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.cookingTimeMinutes = cookingTimeMinutes;
        this.servingSize = servingSize;
        this.difficulty = difficulty;
        this.tags = tags;
        this.createdBy = createdBy;
        this.aiGenerated = aiGenerated;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getTitle() {
        return title != null ? title : name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name != null ? name : title;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public Double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(Double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public Integer getCookingTime() {
        return cookingTime != null ? cookingTime : cookingTimeMinutes;
    }

    public void setCookingTime(Integer cookingTime) {
        this.cookingTime = cookingTime;
    }

    public Double getProtein() {
        return protein;
    }

    public void setProtein(Double protein) {
        this.protein = protein;
    }

    public Double getCarbs() {
        return carbs;
    }

    public void setCarbs(Double carbs) {
        this.carbs = carbs;
    }

    public Double getFat() {
        return fat;
    }

    public void setFat(Double fat) {
        this.fat = fat;
    }

    public Integer getCookingTimeMinutes() {
        return cookingTimeMinutes;
    }

    public void setCookingTimeMinutes(Integer cookingTimeMinutes) {
        this.cookingTimeMinutes = cookingTimeMinutes;
    }

    public Integer getServingSize() {
        return servingSize;
    }

    public void setServingSize(Integer servingSize) {
        this.servingSize = servingSize;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getAuthorId() {
        return authorId != null ? authorId : createdBy;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Boolean getAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(Boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
