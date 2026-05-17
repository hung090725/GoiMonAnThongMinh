package hung.edu.mealmindai.models;

import com.google.firebase.Timestamp;
import java.util.List;

/**
 * User profile stored in the users collection.
 */
public class User {
    // Firestore user document id, usually the Firebase Auth uid.
    private String uid;
    // Firebase Authentication uid.
    private String userId;
    // Basic profile information.
    private String fullName;
    private String email;
    private String avatarUrl;
    private String photoUrl;
    // Profile fields used for meal and budget recommendations.
    private Double height;
    private Double weight;
    private String healthGoal;
    private Double monthlyFoodBudget;
    private Double mealBudget;
    private Integer availableTime;
    private String role;
    // Optional health and preference data for meal recommendations.
    private Integer age;
    private String gender;
    private Double heightCm;
    private Double weightKg;
    private String goal;
    private String dietaryPreference;
    private List<String> allergies;
    private List<String> favoriteCategoryIds;
    // Audit timestamps using Firestore Timestamp for direct deserialization.
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /**
     * Required empty constructor for Firebase.
     */
    public User() {
    }

    public User(String uid, String fullName, String email, String avatarUrl, Double height,
                Double weight, String healthGoal, Double monthlyFoodBudget, String role,
                Timestamp createdAt) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.height = height;
        this.weight = weight;
        this.healthGoal = healthGoal;
        this.monthlyFoodBudget = monthlyFoodBudget;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getHealthGoal() {
        return healthGoal;
    }

    public void setHealthGoal(String healthGoal) {
        this.healthGoal = healthGoal;
    }

    public Double getMonthlyFoodBudget() {
        return monthlyFoodBudget;
    }

    public void setMonthlyFoodBudget(Double monthlyFoodBudget) {
        this.monthlyFoodBudget = monthlyFoodBudget;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Double heightCm) {
        this.heightCm = heightCm;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getDietaryPreference() {
        return dietaryPreference;
    }

    public void setDietaryPreference(String dietaryPreference) {
        this.dietaryPreference = dietaryPreference;
    }

    public List<String> getAllergies() {
        return allergies;
    }

    public void setAllergies(List<String> allergies) {
        this.allergies = allergies;
    }

    public List<String> getFavoriteCategoryIds() {
        return favoriteCategoryIds;
    }

    public void setFavoriteCategoryIds(List<String> favoriteCategoryIds) {
        this.favoriteCategoryIds = favoriteCategoryIds;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Double getMealBudget() {
        return mealBudget;
    }

    public void setMealBudget(Double mealBudget) {
        this.mealBudget = mealBudget;
    }

    public Integer getAvailableTime() {
        return availableTime;
    }

    public void setAvailableTime(Integer availableTime) {
        this.availableTime = availableTime;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
