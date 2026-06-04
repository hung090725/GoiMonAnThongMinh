package hung.edu.mealmindai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchKeywordStore {
    private static final String PREF_NAME = "mealmind_search_keywords";
    private static final String KEY_KEYWORDS = "keywords";
    private static final String SEPARATOR = "\u001F";
    private static final int MAX_ITEMS = 5;

    public static void saveKeyword(Context context, String keyword) {
        if (context == null || TextUtils.isEmpty(keyword)) {
            return;
        }

        String cleanKeyword = keyword.trim();
        if (cleanKeyword.isEmpty()) {
            return;
        }

        List<String> keywords = getKeywords(context);
        removeIgnoreCase(keywords, cleanKeyword);
        keywords.add(0, cleanKeyword);
        while (keywords.size() > MAX_ITEMS) {
            keywords.remove(keywords.size() - 1);
        }
        getPrefs(context).edit().putString(KEY_KEYWORDS, join(keywords)).apply();
    }

    public static List<String> getKeywords(Context context) {
        List<String> keywords = new ArrayList<>();
        if (context == null) {
            return keywords;
        }

        String raw = getPrefs(context).getString(KEY_KEYWORDS, "");
        if (TextUtils.isEmpty(raw)) {
            return keywords;
        }

        for (String token : raw.split(SEPARATOR)) {
            String keyword = token.trim();
            if (!keyword.isEmpty()) {
                keywords.add(keyword);
            }
        }
        return keywords;
    }

    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        getPrefs(context).edit().remove(KEY_KEYWORDS).apply();
    }

    private static void removeIgnoreCase(List<String> values, String candidate) {
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i).equalsIgnoreCase(candidate)) {
                values.remove(i);
            }
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }
}
