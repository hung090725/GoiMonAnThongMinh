package hung.edu.mealmindai.models;

import java.util.List;

/**
 * Recipe data stored in the recipes collection.
 */
public class Recipe {
    // Firestore document id or custom recipe id.
    private String recipeId;
    // Main recipe content shown to users.
    private String title;
    private String description;
    private String imageUrl;
    private String categoryId;
    private List<String> ingredients;
    private List<String> steps;
    // Nutrition and cooking details.
    private Integer calories;
    private Double protein;
    private Double carbs;
    private Double fat;
    private Integer cookingTimeMinutes;
    private Integer servingSize;
    private String difficulty;
    private List<String> tags;
    // Ownership and metadata.
    private String createdBy;
    private Boolean aiGenerated;
    private Long createdAt;
    private Long updatedAt;

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
                  Long createdAt, Long updatedAt) {
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
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Boolean getAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(Boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
