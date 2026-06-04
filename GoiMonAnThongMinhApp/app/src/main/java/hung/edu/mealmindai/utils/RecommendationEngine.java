package hung.edu.mealmindai.utils;

import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.models.RecipeScoreResult;

/**
 * Bộ lọc thông minh tính toán điểm phù hợp (score) cho các món ăn.
 * Dựa trên: Nguyên liệu có sẵn, Mục tiêu sức khỏe, Ngân sách và Thời gian.
 */
public class RecommendationEngine {
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private static final double MAIN_INGREDIENT_BONUS = 10.0;
    private static final Map<String, List<String>> SYNONYM_MAP = buildSynonymMap();

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
        IngredientMatchSummary summary = calculateIngredientMatchSummary(recipe, userIngredients);
        return summary.ingredientScore;
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
            ArrayList<String> missingIngredients = findMissingIngredients(recipe, userIngredients);
            IngredientMatchSummary matchSummary = calculateIngredientMatchSummary(recipe, userIngredients);
            int matchPercent = (int) Math.round(matchSummary.recipeCoverage * 100.0);
            double ingredientScore = matchSummary.ingredientScore;
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
                result.setMissingIngredients(missingIngredients);
                result.setMatchPercent(matchPercent);
                result.setConfidenceLevel(getConfidenceLevel(totalScore));
                result.setCookabilityLevel(getCookabilityLevel(matchPercent));
                result.setReason(buildRecommendationReason(
                        recipe, matchedIngredients, missingIngredients, healthGoal, mealBudget, availableTime));
                recipe.setMatchedIngredients(matchedIngredients);
                recipe.setMissingIngredients(missingIngredients);
                recipe.setMatchPercent(matchPercent);
                recipe.setRecommendationScore(totalScore);
                recipe.setRecommendationReason(result.getReason());
                recipe.setIngredientScore(ingredientScore);
                recipe.setHealthScore(healthScore);
                recipe.setBudgetScore(budgetScore);
                recipe.setTimeScore(timeScore);
                recipe.setFavoriteScore(favoriteScore);
                recipe.setConfidenceLevel(result.getConfidenceLevel());
                recipe.setCookabilityLevel(result.getCookabilityLevel());
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

    public static int calculateMatchPercent(Recipe recipe, List<String> matchedIngredients) {
        return (int) Math.round(calculateDetailedIngredientScore(recipe, matchedIngredients));
    }

    public static ArrayList<String> findMatchedIngredients(Recipe recipe, List<String> userIngredients) {
        ArrayList<String> matches = new ArrayList<>();
        if (recipe == null || userIngredients == null || userIngredients.isEmpty()) {
            return matches;
        }

        List<String> recipeIngredients = safeIngredients(recipe);
        for (String userIngredient : userIngredients) {
            String normalizedUserIngredient = canonicalIngredient(userIngredient);
            if (normalizedUserIngredient.isEmpty()) {
                continue;
            }

            for (String recipeIngredient : recipeIngredients) {
                if (isIngredientMatch(userIngredient, recipeIngredient)) {
                    if (!containsNormalized(matches, recipeIngredient)) {
                        matches.add(recipeIngredient);
                    }
                    break;
                }
            }
        }
        return matches;
    }

    public static ArrayList<String> findMissingIngredients(Recipe recipe, List<String> userIngredients) {
        ArrayList<String> missing = new ArrayList<>();
        if (recipe == null || recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return missing;
        }

        for (String recipeIngredient : recipe.getIngredients()) {
            String normalizedRecipeIngredient = canonicalIngredient(recipeIngredient);
            if (normalizedRecipeIngredient.isEmpty()) {
                continue;
            }

            boolean matched = false;
            if (userIngredients != null) {
                for (String userIngredient : userIngredients) {
                    if (isIngredientMatch(userIngredient, recipeIngredient)) {
                        matched = true;
                        break;
                    }
                }
            }

            if (!matched && !containsNormalized(missing, recipeIngredient)) {
                missing.add(recipeIngredient);
            }
        }
        return missing;
    }

    public static String getConfidenceLevel(double totalScore) {
        if (totalScore >= 80) {
            return "Rất phù hợp ✅";
        } else if (totalScore >= 60) {
            return "Phù hợp 👍";
        } else if (totalScore >= 40) {
            return "Có thể thử 🤔";
        }
        return "Ít phù hợp ⚠️";
    }

    public static String getCookabilityLevel(int matchPercent) {
        if (matchPercent >= 80) {
            return "🟢 Nấu ngay được";
        } else if (matchPercent >= 40) {
            return "🟡 Cần mua thêm ít";
        }
        return "🔴 Cần mua thêm nhiều";
    }

    public static String buildRecommendationReason(Recipe recipe,
                                                   ArrayList<String> matchedIngredients,
                                                   ArrayList<String> missingIngredients,
                                                   String healthGoal,
                                                   int mealBudget,
                                                   int availableTime) {
        StringBuilder reason = new StringBuilder();
        int totalIngredients = recipe != null && recipe.getIngredients() != null
                ? recipe.getIngredients().size() : 0;

        if (matchedIngredients != null && !matchedIngredients.isEmpty()) {
            reason.append("Bạn có ");
            if (totalIngredients > 0) {
                reason.append(matchedIngredients.size()).append("/").append(totalIngredients).append(" nguyên liệu");
            } else {
                reason.append(matchedIngredients.size()).append(" nguyên liệu phù hợp");
            }
            reason.append(": ").append(joinStrings(matchedIngredients)).append(". ");
        } else {
            reason.append("Món có điểm tổng thể phù hợp với hồ sơ của bạn. ");
        }

        double cost = recipe != null && recipe.getEstimatedCost() != null ? recipe.getEstimatedCost() : 0;
        int time = recipe != null && recipe.getCookingTime() != null ? recipe.getCookingTime() : 0;
        if (mealBudget > 0) {
            reason.append("Chi phí khoảng ").append(formatMoney(cost)).append(cost <= mealBudget
                    ? ", phù hợp ngân sách. " : ", hơi vượt ngân sách. ");
        }
        if (availableTime > 0) {
            reason.append("Thời gian ").append(time).append(" phút").append(time <= availableTime
                    ? ", phù hợp thời gian. " : ", hơi lâu hơn mong muốn. ");
        }
        if (healthGoal != null && !healthGoal.trim().isEmpty()) {
            reason.append("Có xét theo mục tiêu ").append(healthGoal).append(".");
        }
        if (missingIngredients != null && !missingIngredients.isEmpty()) {
            reason.append(" Cần bổ sung: ").append(joinLimited(missingIngredients, 3)).append(".");
        }
        return reason.toString().trim();
    }

    private static boolean containsNormalized(List<String> values, String candidate) {
        String normalizedCandidate = normalizeVietnamese(candidate);
        for (String value : values) {
            if (normalizeVietnamese(value).equals(normalizedCandidate)) {
                return true;
            }
        }
        return false;
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

    private static String joinLimited(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        int displayCount = Math.min(values.size(), limit);
        List<String> displayedValues = values.subList(0, displayCount);
        String result = joinStrings(displayedValues);
        if (values.size() > displayCount) {
            result += " và " + (values.size() - displayCount) + " nguyên liệu khác";
        }
        return result;
    }

    private static String formatMoney(double value) {
        return MONEY_FORMAT.format(value).replace(",", ".") + "đ";
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

    private static IngredientMatchSummary calculateIngredientMatchSummary(Recipe recipe, List<String> userIngredients) {
        IngredientMatchSummary summary = new IngredientMatchSummary();
        List<String> recipeIngredients = safeIngredients(recipe);
        List<String> userIngredientList = safeList(userIngredients);
        if (recipeIngredients.isEmpty() || userIngredientList.isEmpty()) {
            return summary;
        }

        Set<String> matchedRecipeKeys = new LinkedHashSet<>();
        Set<String> userKeys = new LinkedHashSet<>();
        Set<String> recipeKeys = new LinkedHashSet<>();

        for (String recipeIngredient : recipeIngredients) {
            recipeKeys.add(canonicalIngredient(recipeIngredient));
        }
        for (String userIngredient : userIngredientList) {
            userKeys.add(canonicalIngredient(userIngredient));
        }

        for (String recipeIngredient : recipeIngredients) {
            for (String userIngredient : userIngredientList) {
                if (isIngredientMatch(userIngredient, recipeIngredient)) {
                    matchedRecipeKeys.add(canonicalIngredient(recipeIngredient));
                    break;
                }
            }
        }

        Set<String> union = new HashSet<>(recipeKeys);
        union.addAll(userKeys);
        summary.recipeCoverage = recipeKeys.isEmpty() ? 0 : (double) matchedRecipeKeys.size() / recipeKeys.size();
        summary.jaccardSimilarity = union.isEmpty() ? 0 : (double) matchedRecipeKeys.size() / union.size();
        summary.mainIngredientBonus = hasMainIngredientMatch(recipeIngredients, userIngredientList) ? MAIN_INGREDIENT_BONUS : 0;
        summary.ingredientScore = Math.min(100.0,
                ((0.70 * summary.recipeCoverage) + (0.30 * summary.jaccardSimilarity)) * 100.0
                        + summary.mainIngredientBonus);
        return summary;
    }

    private static boolean hasMainIngredientMatch(List<String> recipeIngredients, List<String> userIngredients) {
        if (recipeIngredients == null || recipeIngredients.isEmpty()
                || userIngredients == null || userIngredients.isEmpty()) {
            return false;
        }
        int limit = Math.min(2, recipeIngredients.size());
        for (int i = 0; i < limit; i++) {
            String recipeIngredient = recipeIngredients.get(i);
            for (String userIngredient : userIngredients) {
                if (isIngredientMatch(userIngredient, recipeIngredient)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isIngredientMatch(String first, String second) {
        String a = canonicalIngredient(first);
        String b = canonicalIngredient(second);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        if (a.equals(b) || a.contains(b) || b.contains(a)) {
            return true;
        }
        return areSynonyms(a, b);
    }

    private static boolean areSynonyms(String first, String second) {
        for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
            List<String> terms = entry.getValue();
            boolean hasFirst = terms.contains(first);
            boolean hasSecond = terms.contains(second);
            if (hasFirst && hasSecond) {
                return true;
            }
        }
        return false;
    }

    public static String canonicalIngredient(String value) {
        String normalized = normalizeVietnamese(value)
                .replaceAll("[^a-z0-9\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
            if (entry.getValue().contains(normalized)) {
                return entry.getKey();
            }
        }
        return normalized;
    }

    private static List<String> safeIngredients(Recipe recipe) {
        if (recipe == null || recipe.getIngredients() == null) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>();
        for (String ingredient : recipe.getIngredients()) {
            if (ingredient != null && !ingredient.trim().isEmpty()) {
                values.add(ingredient.trim());
            }
        }
        return values;
    }

    private static List<String> safeList(List<String> values) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                result.add(value.trim());
            }
        }
        return result;
    }

    private static Map<String, List<String>> buildSynonymMap() {
        Map<String, List<String>> map = new HashMap<>();
        addSynonyms(map, "thit heo", "thit heo", "thit lon", "thit ba chi", "thit nac");
        addSynonyms(map, "ca chua", "ca chua", "tomato", "ca chua bi", "ca chua cherry");
        addSynonyms(map, "dau hu", "dau hu", "dau phu", "tofu");
        addSynonyms(map, "hanh", "hanh", "hanh la", "hanh tay");
        addSynonyms(map, "toi", "toi", "garlic", "toi bam");
        addSynonyms(map, "nuoc mam", "nuoc mam", "mam", "fish sauce");
        addSynonyms(map, "thit ga", "thit ga", "ga", "uc ga", "dui ga", "chicken");
        addSynonyms(map, "thit bo", "thit bo", "bo", "beef");
        return map;
    }

    private static void addSynonyms(Map<String, List<String>> map, String canonical, String... values) {
        List<String> normalizedValues = new ArrayList<>();
        normalizedValues.add(normalizeVietnamese(canonical));
        for (String value : values) {
            String normalized = normalizeVietnamese(value);
            if (!normalizedValues.contains(normalized)) {
                normalizedValues.add(normalized);
            }
        }
        map.put(normalizeVietnamese(canonical), normalizedValues);
    }

    private static class IngredientMatchSummary {
        double recipeCoverage;
        double jaccardSimilarity;
        double mainIngredientBonus;
        double ingredientScore;
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
        if (recipe.getDescription() != null) {
            searchableTexts.add(recipe.getDescription());
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
