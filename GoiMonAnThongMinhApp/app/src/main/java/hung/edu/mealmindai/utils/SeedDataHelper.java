package hung.edu.mealmindai.utils;

import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to seed sample recipe data into Firestore.
 * Only seeds once when the "recipes" collection is empty.
 */
public class SeedDataHelper {

    private static final String TAG = "SeedDataHelper";
    private static final String COLLECTION_RECIPES = "recipes";

    /**
     * Checks if the "recipes" collection is empty.
     * If empty, seeds 10 sample recipes with status = "approved".
     * Safe to call multiple times — will not duplicate data.
     */
    public static void seedRecipesIfNeeded() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(COLLECTION_RECIPES)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Log.d(TAG, "Collection recipes đã có dữ liệu — bỏ qua seed.");
                        return;
                    }
                    Log.d(TAG, "Collection recipes rỗng — bắt đầu seed dữ liệu mẫu...");
                    insertSampleRecipes(db);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Lỗi kiểm tra collection recipes: " + e.getMessage(), e));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void insertSampleRecipes(FirebaseFirestore db) {
        Map<String, Object>[] recipes = buildSampleRecipes();
        final int total = recipes.length;
        final int[] successCount = {0};
        final int[] failCount = {0};

        for (Map<String, Object> recipe : recipes) {
            db.collection(COLLECTION_RECIPES)
                    .add(recipe)
                    .addOnSuccessListener(documentReference -> {
                        successCount[0]++;
                        Log.d(TAG, "Seed thành công: " + documentReference.getId()
                                + " (" + successCount[0] + "/" + total + ")");
                        if (successCount[0] + failCount[0] == total) {
                            Log.d(TAG, "Seed hoàn tất: " + successCount[0] + "/" + total + " món thành công.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        failCount[0]++;
                        Log.e(TAG, "Seed thất bại cho một món: " + e.getMessage(), e);
                    });
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object>[] buildSampleRecipes() {
        return new Map[]{
                recipe(
                        "Trứng sốt cà chua",
                        "Món trứng chiên thơm ngon kết hợp với sốt cà chua đậm đà, dễ làm và giàu dinh dưỡng.",
                        "https://images.pexels.com/photos/1435860/pexels-photo-1435860.jpeg?auto=compress&cs=tinysrgb&w=1600",
                        180, 15000.0, 15, "Dễ", "cat_egg",
                        Arrays.asList("3 quả trứng gà", "2 quả cà chua", "2 tép tỏi", "Hành lá",
                                "Dầu ăn", "Muối", "Đường", "Nước mắm"),
                        Arrays.asList(
                                "Đập trứng vào bát, đánh đều với chút muối.",
                                "Cà chua rửa sạch, cắt múi cau. Tỏi băm nhỏ.",
                                "Đun nóng dầu, chiên trứng chín vàng, vớt ra.",
                                "Phi thơm tỏi, cho cà chua vào xào mềm.",
                                "Nêm nước mắm, đường cho vừa ăn, cho trứng vào đảo đều.",
                                "Rắc hành lá, dọn ra ăn với cơm nóng."
                        )
                ),
                recipe(
                        "Cơm rang trứng",
                        "Cơm rang trứng kiểu truyền thống — nhanh gọn, thơm ngon, phù hợp cho bữa ăn tiện lợi.",
                        "https://images.pexels.com/photos/723190/pexels-photo-723190.jpeg?auto=compress&cs=tinysrgb&w=1600",
                        350, 10000.0, 10, "Dễ", "cat_rice",
                        Arrays.asList("2 bát cơm nguội", "2 quả trứng gà", "2 tép tỏi",
                                "Hành lá", "Dầu ăn", "Nước tương", "Muối", "Tiêu"),
                        Arrays.asList(
                                "Đập trứng vào bát, đánh tan.",
                                "Phi thơm tỏi băm với dầu ăn.",
                                "Cho trứng vào xào nhanh, dùng đũa xới tơi.",
                                "Cho cơm nguội vào, đảo đều lửa to.",
                                "Nêm nước tương, muối, tiêu cho vừa miệng.",
                                "Rắc hành lá, múc ra đĩa và dùng nóng."
                        )
                ),
                recipe(
                        "Canh rau ngót thịt băm",
                        "Canh rau ngót nấu thịt băm thanh mát, giải nhiệt và bổ dưỡng cho cả gia đình.",
                        "https://images.pexels.com/photos/2092916/pexels-photo-2092916.jpeg?auto=compress&cs=tinysrgb&w=1600",
                        120, 20000.0, 20, "Dễ", "cat_soup",
                        Arrays.asList("200g rau ngót", "150g thịt heo băm", "2 tép tỏi",
                                "Hành khô", "Nước mắm", "Muối", "Tiêu"),
                        Arrays.asList(
                                "Rau ngót lặt lá, rửa sạch.",
                                "Thịt băm ướp nước mắm, tiêu, hành khô băm.",
                                "Đun sôi 600ml nước, cho thịt băm vào, khuấy đều.",
                                "Vớt bọt cho nước trong, nêm muối vừa ăn.",
                                "Cho rau ngót vào, đun sôi lại 2 phút rồi tắt bếp.",
                                "Chan canh ra bát, dùng ngay khi còn nóng."
                        )
                ),
                recipe(
                        "Salad ức gà",
                        "Salad ức gà ăn kiêng thanh đạm, giàu protein, phù hợp cho người tập gym và giảm cân.",
                        "https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=1600",
                        250, 35000.0, 20, "Dễ", "cat_salad",
                        Arrays.asList("200g ức gà", "Xà lách", "Cà chua bi", "Dưa leo",
                                "Hành tây", "Dầu olive", "Chanh", "Muối", "Tiêu"),
                        Arrays.asList(
                                "Ức gà luộc chín với muối và gừng, để nguội rồi xé sợi.",
                                "Rau xà lách rửa sạch, cà chua bi cắt đôi, dưa leo thái lát.",
                                "Hành tây thái mỏng ngâm nước lạnh 10 phút.",
                                "Pha sốt: dầu olive + nước cốt chanh + muối + tiêu.",
                                "Trộn tất cả nguyên liệu cùng sốt, dọn ra đĩa."
                        )
                ),
                recipe(
                        "Mì xào rau củ",
                        "Mì xào rau củ chay thanh đạm, đủ chất xơ, màu sắc hấp dẫn và chế biến nhanh.",
                        "https://images.pexels.com/photos/2347311/pexels-photo-2347311.jpeg?auto=compress&cs=tinysrgb&w=1600",
                        320, 18000.0, 15, "Dễ", "cat_noodle",
                        Arrays.asList("200g mì tươi", "Cà rốt", "Bắp cải", "Nấm", "Đậu que",
                                "Tỏi", "Dầu hào", "Nước tương", "Dầu ăn"),
                        Arrays.asList(
                                "Mì tươi trụng sơ qua nước sôi, vớt ra để ráo.",
                                "Rau củ rửa sạch, thái sợi hoặc cắt vừa ăn.",
                                "Phi thơm tỏi với dầu ăn.",
                                "Cho rau củ vào xào lửa to khoảng 3 phút.",
                                "Cho mì vào, thêm dầu hào và nước tương, đảo đều.",
                                "Nêm lại gia vị, xào thêm 2 phút rồi dọn ra đĩa."
                        )
                ),
                recipe(
                        "Đậu hũ sốt cà",
                        "Đậu hũ non chiên vàng ăn cùng sốt cà chua chua ngọt, món chay ngon cơm.",
                        "https://images.pexels.com/photos/691114/pexels-photo-691114.jpeg?auto=compress&cs=tinysrgb&w=1600",
                        200, 12000.0, 20, "Dễ", "cat_tofu",
                        Arrays.asList("300g đậu hũ non", "2 quả cà chua", "Tỏi", "Hành lá",
                                "Dầu ăn", "Nước tương", "Đường", "Muối"),
                        Arrays.asList(
                                "Đậu hũ cắt miếng vuông, thấm khô bằng khăn giấy.",
                                "Chiên đậu hũ vàng đều hai mặt, vớt ra để ráo dầu.",
                                "Phi thơm tỏi, cho cà chua cắt múi cau vào xào nhừ.",
                                "Nêm nước tương, đường, muối cho vừa ăn.",
                                "Cho đậu hũ vào, đảo nhẹ tay, đun thêm 3 phút.",
                                "Rắc hành lá, dọn ra đĩa ăn với cơm."
                        )
                ),
                recipe(
                        "Cháo thịt băm",
                        "Cháo trắng nấu thịt băm thơm mềm, dễ tiêu hóa, phù hợp cho cả người bệnh và trẻ nhỏ.",
                        "https://images.pexels.com/photos/376464/pexels-photo-376464.jpeg?auto=compress&cs=tinysrgb&w=1600",
                        220, 15000.0, 30, "Dễ", "cat_porridge",
                        Arrays.asList("100g gạo tẻ", "150g thịt heo băm", "Gừng",
                                "Hành lá", "Nước mắm", "Muối", "Tiêu"),
                        Arrays.asList(
                                "Vo gạo sạch, cho vào nồi với 1 lít nước.",
                                "Đun sôi rồi hạ lửa nhỏ, nấu đến khi gạo nở bung.",
                                "Thịt băm ướp nước mắm, tiêu, gừng băm.",
                                "Cho thịt băm vào cháo, khuấy đều tránh vón cục.",
                                "Nêm muối, nước mắm cho vừa ăn.",
                                "Múc cháo ra bát, rắc hành lá và tiêu, dùng nóng."
                        )
                ),
                recipe(
                        "Cá kho tiêu",
                        "Cá kho tiêu đậm đà, thơm mùi tiêu hạt, ăn kèm cơm trắng rất đưa cơm.",
                        "https://images.pexels.com/photos/262959/pexels-photo-262959.jpeg?auto=compress&cs=tinysrgb&w=1600",
                        280, 40000.0, 35, "Trung bình", "cat_fish",
                        Arrays.asList("500g cá (cá basa hoặc cá thu)", "Tiêu hạt", "Tỏi",
                                "Nước mắm", "Đường", "Dầu màu điều", "Hành khô"),
                        Arrays.asList(
                                "Cá làm sạch, cắt khúc, ướp nước mắm + tiêu + đường 20 phút.",
                                "Phi thơm tỏi, hành khô với dầu màu điều.",
                                "Xếp cá vào nồi đất, đổ hỗn hợp dầu hành tỏi lên trên.",
                                "Thêm ít nước, kho lửa vừa cho cá thấm gia vị.",
                                "Hạ lửa nhỏ, kho tiếp 15–20 phút đến khi nước cạn sánh.",
                                "Rắc thêm tiêu hạt, dọn ra ăn với cơm nóng."
                        )
                ),
                recipe(
                        "Gà xào sả ớt",
                        "Gà xào sả ớt cay thơm, màu vàng bắt mắt, hương vị đậm đà đặc trưng của ẩm thực Việt.",
                        "res://gaxaosa",
                        350, 55000.0, 30, "Trung bình", "cat_chicken",
                        Arrays.asList("500g thịt gà", "3 cây sả", "3 quả ớt", "Tỏi",
                                "Nước mắm", "Đường", "Dầu ăn", "Bột nghệ"),
                        Arrays.asList(
                                "Gà chặt miếng vừa ăn, ướp nước mắm + đường + bột nghệ 15 phút.",
                                "Sả đập dập thái nhỏ, tỏi băm, ớt thái lát.",
                                "Đun nóng dầu, phi thơm tỏi và sả.",
                                "Cho gà vào xào lửa lớn đến khi vàng đều.",
                                "Thêm ớt, nêm lại gia vị, đảo đều.",
                                "Xào tiếp 5 phút cho gà chín thấm, dọn ra đĩa."
                        )
                ),
                recipe(
                        "Rau luộc trứng",
                        "Bữa ăn đơn giản, lành mạnh với rau luộc xanh mướt và trứng luộc chấm muối vừng.",
                        "https://images.pexels.com/photos/257816/pexels-photo-257816.jpeg?auto=compress&cs=tinysrgb&w=1600",
                        150, 8000.0, 15, "Dễ", "cat_vegetable",
                        Arrays.asList("200g rau muống hoặc rau cải", "2 quả trứng gà",
                                "Vừng rang", "Muối", "Chanh hoặc me"),
                        Arrays.asList(
                                "Rau nhặt sạch, rửa dưới vòi nước.",
                                "Luộc trứng chín (8 phút với nước sôi), bóc vỏ.",
                                "Đun sôi nước với chút muối, luộc rau khoảng 3 phút.",
                                "Vớt rau ra, xếp đĩa cho đẹp.",
                                "Pha muối vừng: muối + vừng rang + ít nước cốt chanh.",
                                "Bày trứng cắt đôi cùng rau, chấm muối vừng và thưởng thức."
                        )
                )
        };
    }

    /**
     * Builds a Firestore-compatible map for a single recipe document.
     */
    private static Map<String, Object> recipe(
            String name,
            String description,
            String imageUrl,
            int calories,
            double estimatedCost,
            int cookingTime,
            String difficulty,
            String categoryId,
            java.util.List<String> ingredients,
            java.util.List<String> steps
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", description);
        data.put("imageUrl", imageUrl);
        data.put("calories", calories);
        data.put("estimatedCost", estimatedCost);
        data.put("cookingTime", cookingTime);
        data.put("difficulty", difficulty);
        data.put("categoryId", categoryId);
        data.put("ingredients", ingredients);
        data.put("steps", steps);
        data.put("authorId", "system");
        data.put("authorName", "MealMind AI");
        data.put("status", "approved");
        data.put("likeCount", 0);
        data.put("createdAt", FieldValue.serverTimestamp());
        return data;
    }
}
