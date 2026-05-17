package hung.edu.mealmindai.utils;

import android.text.TextUtils;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.models.RecipeScoreResult;
import hung.edu.mealmindai.models.SmartSuggestionResponse;
import hung.edu.mealmindai.models.User;

/**
 * Rule-based assistant that parses natural text and suggests meals without external AI APIs.
 */
public class SmartMealAssistant {
    private static final String TAG = "SmartMealAI";
    private static final Pattern MONEY_K_PATTERN = Pattern.compile("(?:duoi|dưới|khoang|khoảng)?\\s*(\\d{1,4})\\s*k");
    private static final Pattern MONEY_NUMBER_PATTERN = Pattern.compile("(?:duoi|dưới|khoang|khoảng)?\\s*(\\d{5,7})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,3})\\s*(?:phut|phút)");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private static final List<String> COMMON_INGREDIENTS = Arrays.asList(
            "trứng", "cà chua", "cơm", "thịt heo", "thịt gà", "ức gà", "rau",
            "hành", "tỏi", "sả", "ớt", "đậu hũ", "cá", "bí đỏ", "rau ngót",
            "nấm", "mì", "bún", "gạo", "thịt băm", "đậu phụ"
    );
    private static final List<String> INGREDIENT_STOPWORDS = Arrays.asList(
            "tôi", "toi", "mình", "minh", "bạn", "ban", "em", "anh", "chị", "chi",
            "muốn", "muon", "cần", "can", "gợi ý", "goi y", "kiểm tra", "kiem tra",
            "collection", "recipes", "món ăn", "mon an", "món", "mon", "ăn", "an",
            "nấu", "nau", "dữ liệu", "du lieu"
    );

    public SmartSuggestionResponse suggestMeals(String userInput, User userProfile, List<Recipe> recipes) {
        ParsedRequest request = parseInput(userInput, userProfile);
        Log.d(TAG, "input=" + userInput);
        Log.d(TAG, "parsed ingredients=" + request.ingredients
                + " budget=" + request.budget
                + " time=" + request.availableTime
                + " healthGoal=" + request.healthGoal);

        if (recipes == null || recipes.isEmpty()) {
            return new SmartSuggestionResponse("Hiện chưa có dữ liệu món ăn đã được duyệt.",
                    new ArrayList<>(), false);
        }

        List<RecipeScoreResult> scoredResults = RecommendationEngine.calculateDetailedScores(
                recipes,
                request.ingredients,
                request.healthGoal,
                request.budget,
                request.availableTime
        );

        List<RecipeScoreResult> topResults = pickTopUniqueRecipes(scoredResults, 3);
        for (int i = 0; i < topResults.size(); i++) {
            Recipe recipe = topResults.get(i).getRecipe();
            Log.d(TAG, "top " + (i + 1) + ": " + recipe.getName()
                    + " score=" + topResults.get(i).getTotalScore());
        }

        if (topResults.isEmpty()) {
            return new SmartSuggestionResponse(
                    "Hiện mình chưa tìm thấy món phù hợp. Bạn có thể nhập thêm nguyên liệu như trứng, cà chua, cơm, thịt heo hoặc rau.",
                    topResults,
                    false
            );
        }

        return new SmartSuggestionResponse(buildResponseText(topResults, request), topResults, true);
    }

    private List<RecipeScoreResult> pickTopUniqueRecipes(List<RecipeScoreResult> scoredResults, int limit) {
        List<RecipeScoreResult> topResults = new ArrayList<>();
        Set<String> usedRecipeNames = new HashSet<>();
        for (RecipeScoreResult result : scoredResults) {
            if (result == null || result.getRecipe() == null) {
                continue;
            }

            String recipeName = result.getRecipe().getName();
            String normalizedName = RecommendationEngine.normalizeVietnamese(recipeName);
            if (normalizedName.isEmpty() || usedRecipeNames.contains(normalizedName)) {
                continue;
            }

            topResults.add(result);
            usedRecipeNames.add(normalizedName);
            if (topResults.size() == limit) {
                break;
            }
        }
        return topResults;
    }

    private ParsedRequest parseInput(String input, User userProfile) {
        String raw = input == null ? "" : input.trim();
        String normalized = RecommendationEngine.normalizeVietnamese(raw);
        ParsedRequest request = new ParsedRequest();
        request.healthGoal = parseHealthGoal(normalized, userProfile);
        request.budget = parseBudget(normalized, userProfile);
        request.availableTime = parseTime(normalized, userProfile);
        request.ingredients = parseIngredients(raw, normalized);
        return request;
    }

    private String parseHealthGoal(String normalized, User userProfile) {
        if (normalized.contains("giam can")) {
            return "Giảm cân";
        } else if (normalized.contains("tang can")) {
            return "Tăng cân";
        } else if (normalized.contains("can bang")) {
            return "Ăn cân bằng";
        } else if (normalized.contains("tiet kiem")) {
            return "Ăn tiết kiệm";
        } else if (normalized.contains("an nhanh") || normalized.contains("nau nhanh")) {
            return "Ăn nhanh";
        }

        if (userProfile != null) {
            if (!TextUtils.isEmpty(userProfile.getHealthGoal())) {
                return userProfile.getHealthGoal();
            }
            if (!TextUtils.isEmpty(userProfile.getGoal())) {
                return userProfile.getGoal();
            }
        }
        return "";
    }

    private int parseBudget(String normalized, User userProfile) {
        Matcher kMatcher = MONEY_K_PATTERN.matcher(normalized);
        if (kMatcher.find()) {
            return safeParseInt(kMatcher.group(1)) * 1000;
        }

        Matcher numberMatcher = MONEY_NUMBER_PATTERN.matcher(normalized.replace(".", "").replace(",", ""));
        if (numberMatcher.find()) {
            return safeParseInt(numberMatcher.group(1));
        }

        if (userProfile != null && userProfile.getMealBudget() != null) {
            return userProfile.getMealBudget().intValue();
        }
        return 0;
    }

    private int parseTime(String normalized, User userProfile) {
        Matcher matcher = TIME_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return safeParseInt(matcher.group(1));
        }
        if (normalized.contains("nau nhanh") || normalized.contains("an nhanh")) {
            return 20;
        }
        if (userProfile != null && userProfile.getAvailableTime() != null) {
            return userProfile.getAvailableTime();
        }
        return 0;
    }

    private List<String> parseIngredients(String raw, String normalized) {
        List<String> ingredients = new ArrayList<>();
        String lowerRaw = raw.toLowerCase();
        int indexOfCo = RecommendationEngine.normalizeVietnamese(lowerRaw).indexOf("co ");
        String ingredientPart = indexOfCo >= 0 ? lowerRaw.substring(Math.min(lowerRaw.length(), indexOfCo + 3)) : lowerRaw;
        ingredientPart = ingredientPart
                .replaceAll("\\b(với|và|cùng|kèm|thêm|rồi|nữa)\\b", ",")
                .replaceAll("[.;\\n]+", ",");

        for (String token : ingredientPart.split(",")) {
            String trimmed = cleanIngredientToken(token);
            if (isValidIngredientToken(trimmed) && !containsNormalized(ingredients, trimmed)) {
                ingredients.add(trimmed);
            }
        }

        for (String ingredient : COMMON_INGREDIENTS) {
            if (normalized.contains(RecommendationEngine.normalizeVietnamese(ingredient))
                    && !containsNormalized(ingredients, ingredient)) {
                ingredients.add(ingredient);
            }
        }
        return ingredients;
    }

    private String cleanIngredientToken(String value) {
        return value == null ? "" : value
                .replace("tôi có", "")
                .replace("mình có", "")
                .replace("toi co", "")
                .replace("minh co", "")
                .replace("muốn", "")
                .replace("món", "")
                .replace("ăn", "")
                .replace("nấu", "")
                .replaceAll("\\b(dưới|duoi|khoảng|khoang|phút|phut|giảm cân|tăng cân|cân bằng|tiết kiệm)\\b", "")
                .replaceAll("\\d+\\s*k?", "")
                .trim();
    }

    private boolean isValidIngredientToken(String value) {
        String normalized = RecommendationEngine.normalizeVietnamese(value);
        if (normalized.length() < 2) {
            return false;
        }

        for (String stopword : INGREDIENT_STOPWORDS) {
            if (normalized.equals(RecommendationEngine.normalizeVietnamese(stopword))) {
                return false;
            }
        }
        return true;
    }

    private boolean containsNormalized(List<String> values, String candidate) {
        String normalizedCandidate = RecommendationEngine.normalizeVietnamese(candidate);
        for (String value : values) {
            if (RecommendationEngine.normalizeVietnamese(value).equals(normalizedCandidate)) {
                return true;
            }
        }
        return false;
    }

    private String buildResponseText(List<RecipeScoreResult> results, ParsedRequest request) {
        RecipeScoreResult best = results.get(0);
        Recipe recipe = best.getRecipe();
        StringBuilder response = new StringBuilder();
        response.append("Mình gợi ý bạn món ").append(recipe.getName()).append(".\n\n");
        response.append("Vì sao phù hợp:\n");
        response.append("1. ").append(best.getMatchedIngredients().isEmpty()
                ? "Món có điểm phù hợp tốt với hồ sơ của bạn."
                : "Trùng nguyên liệu: " + join(best.getMatchedIngredients()) + ".").append("\n");
        response.append("2. Chi phí khoảng ").append(formatMoney(recipe.getEstimatedCost()))
                .append(", ").append(request.budget > 0 ? "so với ngân sách " + formatMoney((double) request.budget) : "phù hợp để tham khảo").append(".\n");
        response.append("3. Thời gian nấu ").append(recipe.getCookingTime() != null ? recipe.getCookingTime() : 0)
                .append(" phút").append(request.availableTime > 0 ? ", so với thời gian bạn muốn " + request.availableTime + " phút." : ".").append("\n");
        response.append("4. Phù hợp mục tiêu: ")
                .append(TextUtils.isEmpty(request.healthGoal) ? "ăn uống cân bằng." : request.healthGoal + ".").append("\n");

        if (results.size() > 1) {
            response.append("\nGợi ý thêm:");
            for (int i = 1; i < results.size(); i++) {
                response.append("\n- ").append(results.get(i).getRecipe().getName());
            }
        }
        return response.toString();
    }

    private String formatMoney(Double value) {
        double safeValue = value != null ? value : 0;
        return MONEY_FORMAT.format(safeValue).replace(",", ".") + "đ";
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private int safeParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private static class ParsedRequest {
        List<String> ingredients = new ArrayList<>();
        int budget;
        int availableTime;
        String healthGoal = "";
    }
}
