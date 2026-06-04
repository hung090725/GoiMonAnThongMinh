package hung.edu.mealmindai.utils;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hung.edu.mealmindai.models.User;

public class SmartInputParser {
    private static final Pattern MONEY_K_PATTERN = Pattern.compile("(?:duoi|dưới|khoang|khoảng)?\\s*(\\d{1,4})\\s*k");
    private static final Pattern MONEY_NUMBER_PATTERN = Pattern.compile("(?:duoi|dưới|khoang|khoảng)?\\s*(\\d{5,7})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,3})\\s*(?:phut|phút)");
    private static final List<String> COMMON_INGREDIENTS = Arrays.asList(
            "trứng", "cà chua", "thịt heo", "thịt lợn", "thịt gà", "gà", "ức gà",
            "thịt bò", "bò", "đậu hũ", "đậu phụ", "tofu", "hành", "hành lá",
            "hành tây", "tỏi", "nước mắm", "cơm", "rau", "cá", "tôm", "mực",
            "nấm", "mì", "bún", "gạo", "bí đỏ", "rau ngót", "thịt băm"
    );
    private static final List<String> STOPWORDS = Arrays.asList(
            "toi", "minh", "ban", "em", "anh", "chi", "co", "muon", "can",
            "goi y", "tim", "mon", "an", "nau", "duoi", "khoang", "trong",
            "phut", "dang", "giam can", "tang can", "can bang", "tiet kiem"
    );

    public static ParsedInput parse(String input, User userProfile) {
        String raw = input == null ? "" : input.trim();
        String normalized = RecommendationEngine.normalizeVietnamese(raw);
        ParsedInput parsed = new ParsedInput();
        parsed.normalizedText = normalized;
        parsed.healthGoal = parseHealthGoal(normalized, userProfile);
        parsed.maxBudget = parseBudget(normalized, userProfile);
        parsed.maxTime = parseTime(normalized, userProfile);
        parsed.ingredients = parseIngredients(raw, normalized);
        parsed.hasFoodIntent = detectFoodIntent(normalized, parsed);
        return parsed;
    }

    private static String parseHealthGoal(String normalized, User userProfile) {
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

    private static int parseBudget(String normalized, User userProfile) {
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

    private static int parseTime(String normalized, User userProfile) {
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

    private static List<String> parseIngredients(String raw, String normalized) {
        List<String> ingredients = new ArrayList<>();
        String ingredientPart = raw.toLowerCase();
        int startIndex = normalized.indexOf("co ");
        if (startIndex >= 0) {
            ingredientPart = ingredientPart.substring(Math.min(ingredientPart.length(), startIndex + 3));
        }
        ingredientPart = ingredientPart
                .replaceAll("\\b(với|và|cùng|kèm|thêm|rồi|nữa)\\b", ",")
                .replaceAll("[.;\\n]+", ",");

        for (String token : ingredientPart.split(",")) {
            String ingredient = cleanIngredientToken(token);
            if (isValidIngredient(ingredient) && !containsNormalized(ingredients, ingredient)) {
                ingredients.add(ingredient);
            }
        }

        for (String ingredient : COMMON_INGREDIENTS) {
            String normalizedIngredient = RecommendationEngine.normalizeVietnamese(ingredient);
            if (normalized.contains(normalizedIngredient)
                    && !containsNormalized(ingredients, ingredient)) {
                ingredients.add(ingredient);
            }
        }
        return ingredients;
    }

    private static boolean detectFoodIntent(String normalized, ParsedInput parsed) {
        if (!parsed.ingredients.isEmpty()) {
            return true;
        }
        return normalized.contains("goi y")
                || normalized.contains("toi co")
                || normalized.contains("minh co")
                || normalized.contains("tim mon")
                || normalized.contains("nau")
                || normalized.contains("nguyen lieu")
                || normalized.contains("thuc don")
                || normalized.contains("duoi")
                || normalized.contains("khoang")
                || normalized.contains("phut")
                || normalized.contains("giam can")
                || normalized.contains("tang can")
                || normalized.contains("tiet kiem")
                || normalized.contains("an nhanh")
                || normalized.contains("mon");
    }

    private static String cleanIngredientToken(String value) {
        return value == null ? "" : value
                .replace("tôi có", "")
                .replace("mình có", "")
                .replace("toi co", "")
                .replace("minh co", "")
                .replace("gợi ý", "")
                .replace("goi y", "")
                .replace("tìm", "")
                .replace("tim", "")
                .replace("cho tôi", "")
                .replace("cho toi", "")
                .replace("muốn", "")
                .replace("món", "")
                .replace("ăn", "")
                .replace("nấu", "")
                .replaceAll("\\b(dưới|duoi|khoảng|khoang|trong|phút|phut|đang|giảm cân|tăng cân|cân bằng|tiết kiệm)\\b", "")
                .replaceAll("\\d+\\s*k?", "")
                .trim();
    }

    private static boolean isValidIngredient(String value) {
        String normalized = RecommendationEngine.normalizeVietnamese(value);
        if (normalized.length() < 2) {
            return false;
        }
        for (String stopword : STOPWORDS) {
            if (normalized.equals(stopword)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsNormalized(List<String> values, String candidate) {
        String normalizedCandidate = RecommendationEngine.normalizeVietnamese(candidate);
        for (String value : values) {
            if (RecommendationEngine.normalizeVietnamese(value).equals(normalizedCandidate)) {
                return true;
            }
        }
        return false;
    }

    private static int safeParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public static class ParsedInput {
        public List<String> ingredients = new ArrayList<>();
        public int maxBudget;
        public int maxTime;
        public String healthGoal = "";
        public String normalizedText = "";
        public boolean hasFoodIntent;
    }
}
