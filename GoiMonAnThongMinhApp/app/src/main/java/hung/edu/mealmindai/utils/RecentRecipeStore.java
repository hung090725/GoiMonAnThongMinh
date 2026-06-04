package hung.edu.mealmindai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class RecentRecipeStore {
    private static final String PREF_NAME = "mealmind_recent_recipes";
    private static final String KEY_RECENT_IDS = "recent_recipe_ids";
    private static final int MAX_ITEMS = 8;

    public static void saveViewedRecipe(Context context, String recipeId) {
        if (context == null || TextUtils.isEmpty(recipeId)) {
            return;
        }

        List<String> ids = getRecentRecipeIds(context);
        ids.remove(recipeId);
        ids.add(0, recipeId);
        while (ids.size() > MAX_ITEMS) {
            ids.remove(ids.size() - 1);
        }
        getPrefs(context).edit().putString(KEY_RECENT_IDS, join(ids)).apply();
    }

    public static List<String> getRecentRecipeIds(Context context) {
        List<String> ids = new ArrayList<>();
        if (context == null) {
            return ids;
        }

        String raw = getPrefs(context).getString(KEY_RECENT_IDS, "");
        if (TextUtils.isEmpty(raw)) {
            return ids;
        }

        for (String token : raw.split(",")) {
            String id = token.trim();
            if (!id.isEmpty() && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static String join(List<String> ids) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(ids.get(i));
        }
        return builder.toString();
    }
}
