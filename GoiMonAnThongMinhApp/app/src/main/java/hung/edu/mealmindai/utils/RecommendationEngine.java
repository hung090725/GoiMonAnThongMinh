package hung.edu.mealmindai.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.models.RecipeScoreResult;

/**
 * Bộ lọc thông minh tính toán điểm phù hợp (score) cho các món ăn.
 * Dựa trên: Nguyên liệu có sẵn, Mục tiêu sức khỏe, Ngân sách và Thời gian.
 */
public class RecommendationEngine {
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

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
        
        List<String> searchableTexts = buildSearchableTexts(recipe);
        if (searchableTexts.isEmpty()) return 0;

        int matchedCount = 0;
        for (String userIng : userIngredients) {
            String ui = normalizeVietnamese(userIng);
            if (ui.isEmpty()) {
                continue;
            }

            for (String recipeText : searchableTexts) {
                String normalizedRecipeText = normalizeVietnamese(recipeText);
                if (isSearchMatch(ui, normalizedRecipeText)) {
                    matchedCount++;
                    break; 
                }
            }
        }

        // Score theo số từ khóa người dùng nhập, để nhập ít nguyên liệu vẫn có kết quả.
        return ((double) matchedCount / userIngredients.size()) * 100.0;
    }

    public static List<RecipeScoreResult> calculateDetailedScores(
            List<Recipe> recipes,
            List<String> userIngredients,
            String healthGoal,
            int mealBudget,
            int availableTime
    ) {
        List<RecipeScoreResult> results = new ArrayList<>();
        if (recipes == null || recipes.isEmpty()) {
            return results;
        }

        for (Recipe recipe : recipes) {
            ArrayList<String> matchedIngredients = findMatchedIngredients(recipe, userIngredients);
            double ingredientScore = calculateDetailedIngredientScore(recipe, matchedIngredients);
            double healthScore = calculateHealthScore(recipe, healthGoal);
            double budgetScore = calculateBudgetScore(recipe, mealBudget);
            double timeScore = calculateTimeScore(recipe, availableTime);
            double favoriteScore = 70.0;
            double totalScore = (ingredientScore * 0.40)
                    + (healthScore * 0.20)
                    + (budgetScore * 0.15)
                    + (timeScore * 0.15)
                    + (favoriteScore * 0.10);

            if (ingredientScore > 0 || totalScore >= 60.0) {
                RecipeScoreResult result = new RecipeScoreResult();
                result.setRecipe(recipe);
                result.setIngredientScore(ingredientScore);
                result.setHealthScore(healthScore);
                result.setBudgetScore(budgetScore);
                result.setTimeScore(timeScore);
                result.setFavoriteScore(favoriteScore);
                result.setTotalScore(totalScore);
                result.setMatchedIngredients(matchedIngredients);
                result.setReason(buildReason(recipe, matchedIngredients, healthGoal, mealBudget, availableTime));
                results.add(result);
            }
        }

        Collections.sort(results, (first, second) ->
                Double.compare(second.getTotalScore(), first.getTotalScore()));
        return results;
    }

    private static double calculateDetailedIngredientScore(Recipe recipe, List<String> matchedIngredients) {
        if (matchedIngredients == null || matchedIngredients.isEmpty()) {
            return 0.0;
        }

        int totalIngredients = recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()
                ? recipe.getIngredients().size()
                : matchedIngredients.size();
        return Math.min(100.0, ((double) matchedIngredients.size() / totalIngredients) * 100.0);
    }

    private static ArrayList<String> findMatchedIngredients(Recipe recipe, List<String> userIngredients) {
        ArrayList<String> matches = new ArrayList<>();
        if (recipe == null || userIngredients == null || userIngredients.isEmpty()) {
            return matches;
        }

        List<String> searchableTexts = buildSearchableTexts(recipe);
        for (String userIngredient : userIngredients) {
            String normalizedUserIngredient = normalizeVietnamese(userIngredient);
            if (normalizedUserIngredient.isEmpty()) {
                continue;
            }

            for (String recipeText : searchableTexts) {
                String normalizedRecipeText = normalizeVietnamese(recipeText);
                if (isSearchMatch(normalizedUserIngredient, normalizedRecipeText)) {
                    if (!matches.contains(userIngredient)) {
                        matches.add(userIngredient);
                    }
                    break;
                }
            }
        }
        return matches;
    }

    private static String buildReason(Recipe recipe, ArrayList<String> matchedIngredients,
                                      String healthGoal, int mealBudget, int availableTime) {
        StringBuilder reason = new StringBuilder();
        if (matchedIngredients != null && !matchedIngredients.isEmpty()) {
            reason.append("Trùng nguyên liệu: ").append(joinStrings(matchedIngredients)).append(". ");
        } else {
            reason.append("Món có điểm tổng thể phù hợp với hồ sơ của bạn. ");
        }

        double cost = recipe.getEstimatedCost() != null ? recipe.getEstimatedCost() : 0;
        int time = recipe.getCookingTime() != null ? recipe.getCookingTime() : 0;
        if (mealBudget > 0) {
            reason.append(cost <= mealBudget ? "Chi phí phù hợp ngân sách. " : "Chi phí hơi vượt ngân sách. ");
        }
        if (availableTime > 0) {
            reason.append(time <= availableTime ? "Thời gian nấu phù hợp. " : "Thời gian nấu hơi lâu hơn mong muốn. ");
        }
        if (healthGoal != null && !healthGoal.trim().isEmpty()) {
            reason.append("Có xét theo mục tiêu ").append(healthGoal).append(".");
        }
        return reason.toString().trim();
    }

    private static String joinStrings(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    public static String normalizeVietnamese(String value) {
        if (value == null) return "";

        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return normalized
                .replace("đ", "d")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isSearchMatch(String userTerm, String recipeText) {
        if (userTerm.isEmpty() || recipeText.isEmpty()) {
            return false;
        }

        // Từ khóa rất ngắn như "ga" không được khớp rộng với "gao".
        if (userTerm.length() <= 2) {
            for (String token : recipeText.split("[^a-z0-9]+")) {
                if (token.equals(userTerm)) {
                    return true;
                }
            }
            return false;
        }

        return recipeText.contains(userTerm) || userTerm.contains(recipeText);
    }

    private static List<String> buildSearchableTexts(Recipe recipe) {
        List<String> searchableTexts = new ArrayList<>();
        if (recipe == null) {
            return searchableTexts;
        }

        if (recipe.getIngredients() != null) {
            searchableTexts.addAll(recipe.getIngredients());
        }
        if (recipe.getName() != null) {
            searchableTexts.add(recipe.getName());
        }
        if (recipe.getTitle() != null) {
            searchableTexts.add(recipe.getTitle());
        }
        if (recipe.getTags() != null) {
            searchableTexts.addAll(recipe.getTags());
        }
        return searchableTexts;
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
