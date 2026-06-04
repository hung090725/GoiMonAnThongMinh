package hung.edu.mealmindai.models;

import java.util.ArrayList;

/**
 * Detailed score breakdown for one suggested recipe.
 */
public class RecipeScoreResult {
    private Recipe recipe;
    private double totalScore;
    private double ingredientScore;
    private double healthScore;
    private double budgetScore;
    private double timeScore;
    private double favoriteScore;
    private ArrayList<String> matchedIngredients;
    private ArrayList<String> missingIngredients;
    private int matchPercent;
    private String reason;
    private String confidenceLevel;
    private String cookabilityLevel;

    public RecipeScoreResult() {
        matchedIngredients = new ArrayList<>();
        missingIngredients = new ArrayList<>();
    }

    public RecipeScoreResult(Recipe recipe, double totalScore, double ingredientScore,
                             double healthScore, double budgetScore, double timeScore,
                             double favoriteScore, ArrayList<String> matchedIngredients,
                             String reason) {
        this.recipe = recipe;
        this.totalScore = totalScore;
        this.ingredientScore = ingredientScore;
        this.healthScore = healthScore;
        this.budgetScore = budgetScore;
        this.timeScore = timeScore;
        this.favoriteScore = favoriteScore;
        this.matchedIngredients = matchedIngredients;
        this.missingIngredients = new ArrayList<>();
        this.reason = reason;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public double getIngredientScore() {
        return ingredientScore;
    }

    public void setIngredientScore(double ingredientScore) {
        this.ingredientScore = ingredientScore;
    }

    public double getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(double healthScore) {
        this.healthScore = healthScore;
    }

    public double getBudgetScore() {
        return budgetScore;
    }

    public void setBudgetScore(double budgetScore) {
        this.budgetScore = budgetScore;
    }

    public double getTimeScore() {
        return timeScore;
    }

    public void setTimeScore(double timeScore) {
        this.timeScore = timeScore;
    }

    public double getFavoriteScore() {
        return favoriteScore;
    }

    public void setFavoriteScore(double favoriteScore) {
        this.favoriteScore = favoriteScore;
    }

    public ArrayList<String> getMatchedIngredients() {
        return matchedIngredients;
    }

    public void setMatchedIngredients(ArrayList<String> matchedIngredients) {
        this.matchedIngredients = matchedIngredients;
    }

    public ArrayList<String> getMissingIngredients() {
        return missingIngredients;
    }

    public void setMissingIngredients(ArrayList<String> missingIngredients) {
        this.missingIngredients = missingIngredients;
    }

    public int getMatchPercent() {
        return matchPercent;
    }

    public void setMatchPercent(int matchPercent) {
        this.matchPercent = matchPercent;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getCookabilityLevel() {
        return cookabilityLevel;
    }

    public void setCookabilityLevel(String cookabilityLevel) {
        this.cookabilityLevel = cookabilityLevel;
    }
}
