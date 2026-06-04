package hung.edu.mealmindai.utils;

import android.text.TextUtils;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hung.edu.mealmindai.models.Recipe;
import hung.edu.mealmindai.models.RecipeScoreResult;
import hung.edu.mealmindai.models.SmartSuggestionResponse;
import hung.edu.mealmindai.models.User;

/**
 * Rule-based assistant that parses natural text and suggests meals without external AI APIs.
 */
public class SmartMealAssistant {
    private static final String TAG = "SmartMealAI";
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");

    public SmartSuggestionResponse suggestMeals(String userInput, User userProfile, List<Recipe> recipes) {
        ParsedRequest request = parseInput(userInput, userProfile);
        Log.d(TAG, "input=" + userInput);
        Log.d(TAG, "parsed ingredients=" + request.ingredients
                + " budget=" + request.budget
                + " time=" + request.availableTime
                + " healthGoal=" + request.healthGoal);

        String directAnswer = buildDirectAnswerIfNeeded(request);
        if (!TextUtils.isEmpty(directAnswer)) {
            return new SmartSuggestionResponse(directAnswer, new ArrayList<>(), false);
        }

        if (!request.hasFoodIntent) {
            return new SmartSuggestionResponse(buildFallbackGuide(), new ArrayList<>(), false);
        }

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
                    "Mình chưa tìm thấy món thật sự phù hợp với dữ liệu hiện có.\n\nBạn thử nhập rõ hơn theo mẫu:\n- Tôi có trứng, cà chua, dưới 30k\n- Món nấu nhanh 15 phút\n- Gợi ý món giảm cân",
                    topResults,
                    false
            );
        }

        if (!request.ingredients.isEmpty()
                && topResults.get(0).getIngredientScore() <= 0) {
            return new SmartSuggestionResponse(
                    "Mình chưa tìm thấy món khớp với nguyên liệu bạn nhập.\n\nBạn có thể thử nguyên liệu khác hoặc dùng nút Tủ lạnh của tôi để tìm từ danh sách nguyên liệu đã lưu.",
                    new ArrayList<>(),
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
        SmartInputParser.ParsedInput parsedInput = SmartInputParser.parse(input, userProfile);
        ParsedRequest request = new ParsedRequest();
        request.healthGoal = parsedInput.healthGoal;
        request.budget = parsedInput.maxBudget;
        request.availableTime = parsedInput.maxTime;
        request.ingredients = parsedInput.ingredients;
        request.normalizedText = parsedInput.normalizedText;
        request.hasFoodIntent = parsedInput.hasFoodIntent;
        return request;
    }

    private String buildDirectAnswerIfNeeded(ParsedRequest request) {
        String normalized = request.normalizedText;
        if (TextUtils.isEmpty(normalized)) {
            return "";
        }

        if (containsAny(normalized, "xin chao", "chao", "hello", "hi ")) {
            return "Chào bạn, mình là MealMind AI. Mình có thể gợi ý món theo nguyên liệu, ngân sách, thời gian nấu và mục tiêu sức khỏe.\n\nVí dụ: Tôi có trứng, cà chua, muốn ăn dưới 30k.";
        }

        if (containsAny(normalized, "ban la ai", "ai vay", "mealmind la gi", "tro ly")) {
            return "Mình là trợ lý gợi ý món ăn của MealMind AI. Mình không gọi API AI bên ngoài; câu trả lời được tạo bằng rule-based/scoring dựa trên dữ liệu món đã duyệt trong Firestore.";
        }

        if (containsAny(normalized, "cach dung", "huong dan", "help", "giup")) {
            return buildFallbackGuide();
        }

        if (containsAny(normalized, "tu lanh", "pantry", "nguyen lieu da luu")) {
            return "Bạn có thể vào Hồ sơ > Tủ lạnh của tôi để lưu nguyên liệu đang có. Sau đó qua màn Tìm món và bấm Dùng nguyên liệu từ tủ lạnh, app sẽ tự điền nguyên liệu và tìm món phù hợp.";
        }

        if (containsAny(normalized, "ke hoach", "meal plan", "hom nay")) {
            return "Kế hoạch hôm nay dùng để lưu món theo bữa sáng, bữa trưa và bữa tối. Khi nấu xong, bạn tick món để theo dõi tiến độ hoàn thành trong ngày.";
        }

        if (containsAny(normalized, "danh sach mua", "shopping", "can mua")) {
            return "Danh sách mua lấy nguyên liệu từ món ăn đang xem. Bạn có thể tick từng nguyên liệu đã chuẩn bị, app sẽ lưu lại trạng thái đã tick.";
        }

        if (containsAny(normalized, "yeu thich", "mon da luu", "favorite")) {
            return "Món yêu thích được lưu riêng theo tài khoản đang đăng nhập. Bạn bấm biểu tượng tim ở chi tiết món để thêm hoặc bấm lại để bỏ yêu thích.";
        }

        if (containsAny(normalized, "admin", "duyet", "tu choi", "pending", "approved")) {
            return "Luồng cộng đồng hoạt động như sau: user gửi công thức với trạng thái pending, admin kiểm tra rồi chuyển sang approved hoặc rejected. Nếu rejected, app lưu lý do từ chối để user xem lại.";
        }

        if (containsAny(normalized, "cam on", "thanks", "thank")) {
            return "Không có gì. Bạn cứ nhập nguyên liệu hoặc mục tiêu ăn uống, mình sẽ gợi ý món phù hợp từ dữ liệu của app.";
        }
        return "";
    }

    private String buildFallbackGuide() {
        return "Mình có thể hỗ trợ theo các kiểu câu hỏi sau:\n\n"
                + "- Gợi ý món: Tôi có trứng, cà chua, dưới 30k\n"
                + "- Theo thời gian: Món nấu nhanh 15 phút\n"
                + "- Theo sức khỏe: Gợi ý món giảm cân\n"
                + "- Theo tính năng: Cách dùng tủ lạnh, kế hoạch hôm nay, danh sách mua\n\n"
                + "Mình sẽ trả lời dựa trên dữ liệu món đã được duyệt và thuật toán rule-based/scoring của app.";
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String buildResponseText(List<RecipeScoreResult> results, ParsedRequest request) {
        RecipeScoreResult best = results.get(0);
        Recipe recipe = best.getRecipe();
        StringBuilder response = new StringBuilder();
        double totalScore = best.getTotalScore();
        int matchPercent = best.getMatchPercent();

        if (totalScore >= 75) {
            response.append("Mình gợi ý bạn nấu ").append(recipe.getName()).append(".");
        } else if (totalScore >= 55) {
            response.append("Mình tìm được món khá gần với nhu cầu của bạn: ")
                    .append(recipe.getName()).append(".");
        } else {
            response.append("Món gần nhất mình tìm được là ")
                    .append(recipe.getName())
                    .append(", nhưng bạn sẽ cần chuẩn bị thêm một số nguyên liệu.");
        }
        response.append("\n\n");

        if (request.ingredients != null && !request.ingredients.isEmpty()) {
            response.append("Bạn đang có: ").append(join(request.ingredients)).append(".\n");
        }

        if (best.getMatchedIngredients() != null && !best.getMatchedIngredients().isEmpty()) {
            response.append("Món này dùng được: ")
                    .append(joinLimited(best.getMatchedIngredients(), 4))
                    .append(".\n");
        }

        if (best.getMissingIngredients() != null && !best.getMissingIngredients().isEmpty()) {
            response.append("Cần mua thêm: ")
                    .append(joinLimited(best.getMissingIngredients(), 4))
                    .append(".\n");
        }

        response.append("\nĐánh giá nhanh: ");
        if (matchPercent >= 80) {
            response.append("bạn gần như có đủ nguyên liệu để nấu ngay.");
        } else if (matchPercent >= 40) {
            response.append("bạn đã có một phần nguyên liệu, chỉ cần mua thêm ít.");
        } else {
            response.append("món này cần mua thêm khá nhiều, nên phù hợp khi bạn vẫn muốn nấu món gà.");
        }

        appendBudgetNote(response, recipe, request);
        appendTimeNote(response, recipe, request);
        appendHealthNote(response, request);

        if (results.size() > 1) {
            response.append("\n\nBạn cũng có thể xem thêm:");
            for (int i = 1; i < results.size(); i++) {
                response.append("\n- ").append(results.get(i).getRecipe().getName());
            }
        }
        return response.toString();
    }

    private void appendBudgetNote(StringBuilder response, Recipe recipe, ParsedRequest request) {
        if (request.budget <= 0) {
            return;
        }
        double cost = recipe.getEstimatedCost() != null ? recipe.getEstimatedCost() : 0;
        response.append(" Chi phí khoảng ").append(formatMoney(cost));
        if (cost <= request.budget) {
            response.append(", nằm trong ngân sách ").append(formatMoney((double) request.budget)).append(".");
        } else {
            response.append(", hơi vượt ngân sách ").append(formatMoney((double) request.budget)).append(".");
        }
    }

    private void appendTimeNote(StringBuilder response, Recipe recipe, ParsedRequest request) {
        if (request.availableTime <= 0) {
            return;
        }
        int time = recipe.getCookingTime() != null ? recipe.getCookingTime() : 0;
        response.append(" Thời gian nấu khoảng ").append(time).append(" phút");
        if (time <= request.availableTime) {
            response.append(", kịp với thời gian bạn muốn.");
        } else {
            response.append(", lâu hơn thời gian bạn muốn một chút.");
        }
    }

    private void appendHealthNote(StringBuilder response, ParsedRequest request) {
        if (TextUtils.isEmpty(request.healthGoal)) {
            return;
        }
        response.append(" Mình cũng đã xét theo mục tiêu ").append(request.healthGoal).append(".");
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

    private String joinLimited(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        int displayCount = Math.min(values.size(), limit);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < displayCount; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        if (values.size() > displayCount) {
            builder.append(" và ").append(values.size() - displayCount).append(" nguyên liệu khác");
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
        String normalizedText = "";
        boolean hasFoodIntent;
    }
}
