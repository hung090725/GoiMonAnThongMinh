package hung.edu.mealmindai.models;

/**
 * Planned meal stored in the meal_plans collection.
 */
public class MealPlan {
    // Firestore document id or custom meal plan id.
    private String mealPlanId;
    // User, recipe, and schedule details.
    private String userId;
    private String recipeId;
    private String recipeName;
    private String mealDate;
    private String mealType;
    private Integer servings;
    private String notes;
    private Boolean completed;
    private Long createdAt;
    private Long updatedAt;

    /**
     * Required empty constructor for Firebase.
     */
    public MealPlan() {
    }

    public MealPlan(String mealPlanId, String userId, String recipeId, String mealDate,
                    String mealType, Integer servings, String notes, Boolean completed,
                    Long createdAt, Long updatedAt) {
        this.mealPlanId = mealPlanId;
        this.userId = userId;
        this.recipeId = recipeId;
        this.mealDate = mealDate;
        this.mealType = mealType;
        this.servings = servings;
        this.notes = notes;
        this.completed = completed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getMealPlanId() {
        return mealPlanId;
    }

    public void setMealPlanId(String mealPlanId) {
        this.mealPlanId = mealPlanId;
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

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getMealDate() {
        return mealDate;
    }

    public void setMealDate(String mealDate) {
        this.mealDate = mealDate;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public Integer getServings() {
        return servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
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
