package hung.edu.mealmindai.utils;

import java.util.Collections;
import java.util.List;
import hung.edu.mealmindai.models.Recipe;

/**
 * Bộ lọc thông minh tính toán điểm phù hợp (score) cho các món ăn.
 * Dựa trên: Nguyên liệu có sẵn, Mục tiêu sức khỏe, Ngân sách và Thời gian.
 */
public class RecommendationEngine {

    /**
     * Tính tổng điểm cho một món ăn.
     */
    public static double calculateScore(Recipe recipe, List<String> userIngredients, 
                                      String healthGoal, double userBudget, int availableTime) {
        
        double ingredientScore = calculateIngredientScore(recipe, userIngredients);
        double healthScore = calculateHealthScore(recipe, healthGoal);
        double budgetScore = calculateBudgetScore(recipe, userBudget);
        double timeScore = calculateTimeScore(recipe, availableTime);
        double favoriteScore = 70.0; // Mặc định ở B10

        // Công thức tính điểm tổng hợp theo trọng số
        return (ingredientScore * 0.40) + 
               (healthScore * 0.20) + 
               (budgetScore * 0.15) + 
               (timeScore * 0.15) + 
               (favoriteScore * 0.10);
    }

    /**
     * Sắp xếp danh sách món ăn theo điểm giảm dần.
     */
    public static void sortRecipesByScore(List<RecipeWithScore> recipes) {
        Collections.sort(recipes, (r1, r2) -> Double.compare(r2.score, r1.score));
    }

    public static double calculateIngredientScore(Recipe recipe, List<String> userIngredients) {
        if (userIngredients == null || userIngredients.isEmpty()) return 0;
        
        List<String> recipeIngredients = recipe.getIngredients();
        if (recipeIngredients == null || recipeIngredients.isEmpty()) return 0;

        int matchedCount = 0;
        for (String userIng : userIngredients) {
            String ui = userIng.trim().toLowerCase();
            for (String recipeIng : recipeIngredients) {
                if (recipeIng.toLowerCase().contains(ui)) {
                    matchedCount++;
                    break; 
                }
            }
        }

        // Score = (số lượng khớp / tổng số nguyên liệu món đó) * 100
        return ((double) matchedCount / recipeIngredients.size()) * 100.0;
    }

    public static double calculateHealthScore(Recipe recipe, String healthGoal) {
        if (healthGoal == null || healthGoal.isEmpty()) return 70.0;

        int calories = recipe.getCalories() != null ? recipe.getCalories() : 0;
        
        switch (healthGoal) {
            case "Giảm cân":
                return (calories <= 400) ? 100.0 : (calories < 600 ? 70.0 : 40.0);
            case "Tăng cân":
                return (calories >= 450) ? 100.0 : (calories > 300 ? 70.0 : 40.0);
            case "Ăn cân bằng":
                return (calories >= 300 && calories <= 600) ? 100.0 : 60.0;
            case "Ăn tiết kiệm":
                // Ưu tiên món rẻ
                return (recipe.getEstimatedCost() != null && recipe.getEstimatedCost() < 25000) ? 100.0 : 60.0;
            case "Ăn nhanh":
                // Ưu tiên món thời gian nấu thấp
                return (recipe.getCookingTime() != null && recipe.getCookingTime() <= 15) ? 100.0 : 60.0;
            default:
                return 70.0;
        }
    }

    public static double calculateBudgetScore(Recipe recipe, double userBudget) {
        if (userBudget <= 0) return 70.0;
        
        double cost = recipe.getEstimatedCost() != null ? recipe.getEstimatedCost() : 0.0;
        if (cost <= userBudget) return 100.0;
        
        // Vượt ngân sách -> trừ điểm tỷ lệ thuận với mức vượt (tối thiểu 0)
        double penalty = ((cost - userBudget) / userBudget) * 50.0;
        return Math.max(0, 100.0 - penalty);
    }

    public static double calculateTimeScore(Recipe recipe, int availableTime) {
        if (availableTime <= 0) return 70.0;
        
        int cookTime = recipe.getCookingTime() != null ? recipe.getCookingTime() : 0;
        if (cookTime <= availableTime) return 100.0;
        
        // Vượt thời gian -> trừ điểm (tối thiểu 0)
        double penalty = ((double)(cookTime - availableTime) / availableTime) * 50.0;
        return Math.max(0, 100.0 - penalty);
    }

    /**
     * Lớp wrapper để giữ thông tin Recipe kèm theo điểm số.
     */
    public static class RecipeWithScore {
        public Recipe recipe;
        public double score;

        public RecipeWithScore(Recipe recipe, double score) {
            this.recipe = recipe;
            this.score = score;
        }
    }
}
