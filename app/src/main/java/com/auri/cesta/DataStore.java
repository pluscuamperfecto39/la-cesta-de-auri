package com.auri.cesta;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Almacenamiento local sencillo: todos los datos permanecen en el teléfono. */
public final class DataStore {
    private static final String PREFS = "auri_basket_data";
    private static final String KEY_BASKET = "basket";
    private static final String KEY_FUTURE = "future";
    private static final String KEY_FREQUENT = "frequent";

    private final Context appContext;
    private final SharedPreferences prefs;

    public DataStore(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static final class BasketItem {
        public long id;
        public String name;
        public String category;
        public String quantity;
        public boolean checked;
        public boolean counted;

        public BasketItem(long id, String name, String category, String quantity) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.quantity = quantity;
        }
    }

    public static final class FutureItem {
        public long id;
        public String title;
        public long whenMillis;
        public boolean notify;

        public FutureItem(long id, String title, long whenMillis, boolean notify) {
            this.id = id;
            this.title = title;
            this.whenMillis = whenMillis;
            this.notify = notify;
        }
    }

    public static final class FrequentItem {
        public String name;
        public String category;
        public int count;

        public FrequentItem(String name, String category, int count) {
            this.name = name;
            this.category = category;
            this.count = count;
        }
    }

    public List<BasketItem> getBasket() {
        List<BasketItem> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_BASKET, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                BasketItem item = new BasketItem(
                        object.optLong("id"),
                        object.optString("name"),
                        object.optString("category", "Otros"),
                        object.optString("quantity", "1 ud.")
                );
                item.checked = object.optBoolean("checked");
                item.counted = object.optBoolean("counted");
                result.add(item);
            }
        } catch (JSONException ignored) {
            // Si un dato quedara incompleto, la app sigue funcionando con una cesta vacía.
        }
        return result;
    }

    public void saveBasket(List<BasketItem> items) {
        JSONArray array = new JSONArray();
        for (BasketItem item : items) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", item.id);
                object.put("name", item.name);
                object.put("category", item.category);
                object.put("quantity", item.quantity);
                object.put("checked", item.checked);
                object.put("counted", item.counted);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_BASKET, array.toString()).apply();
        BasketWidgetProvider.refreshAll(appContext);
    }

    public void addBasketItem(String name, String category, String quantity) {
        List<BasketItem> items = getBasket();
        items.add(0, new BasketItem(uniqueId(), tidy(name), category, tidyQuantity(quantity)));
        saveBasket(items);
    }

    public void importBasketItems(List<BasketItem> imported) {
        List<BasketItem> items = getBasket();
        for (int i = imported.size() - 1; i >= 0; i--) {
            BasketItem source = imported.get(i);
            items.add(0, new BasketItem(
                    uniqueId(),
                    tidy(source.name),
                    source.category,
                    tidyQuantity(source.quantity)
            ));
        }
        saveBasket(items);
    }

    public List<FutureItem> getFuture() {
        List<FutureItem> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_FUTURE, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                result.add(new FutureItem(
                        object.optLong("id"),
                        object.optString("title"),
                        object.optLong("when"),
                        object.optBoolean("notify", true)
                ));
            }
        } catch (JSONException ignored) {
        }
        Collections.sort(result, (left, right) -> Long.compare(left.whenMillis, right.whenMillis));
        return result;
    }

    public void saveFuture(List<FutureItem> items) {
        JSONArray array = new JSONArray();
        for (FutureItem item : items) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", item.id);
                object.put("title", item.title);
                object.put("when", item.whenMillis);
                object.put("notify", item.notify);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_FUTURE, array.toString()).apply();
    }

    public FutureItem addFutureItem(String title, long whenMillis, boolean notify) {
        List<FutureItem> items = getFuture();
        FutureItem item = new FutureItem(uniqueId(), tidy(title), whenMillis, notify);
        items.add(item);
        saveFuture(items);
        return item;
    }

    public void incrementFrequent(String name, String category) {
        try {
            JSONObject all = new JSONObject(prefs.getString(KEY_FREQUENT, "{}"));
            String key = normalize(name);
            JSONObject entry = all.optJSONObject(key);
            if (entry == null) entry = new JSONObject();
            entry.put("name", tidy(name));
            entry.put("category", category);
            entry.put("count", entry.optInt("count") + 1);
            all.put(key, entry);
            prefs.edit().putString(KEY_FREQUENT, all.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public List<FrequentItem> getFrequent() {
        List<FrequentItem> result = new ArrayList<>();
        try {
            JSONObject all = new JSONObject(prefs.getString(KEY_FREQUENT, "{}"));
            JSONArray names = all.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    JSONObject entry = all.optJSONObject(names.getString(i));
                    if (entry != null) {
                        result.add(new FrequentItem(
                                entry.optString("name"),
                                entry.optString("category", "Otros"),
                                entry.optInt("count")
                        ));
                    }
                }
            }
        } catch (JSONException ignored) {
        }
        Collections.sort(result, (left, right) -> Integer.compare(right.count, left.count));
        return result;
    }

    private static String tidy(String value) {
        String trimmed = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) return trimmed;
        return trimmed.substring(0, 1).toUpperCase(Locale.getDefault()) + trimmed.substring(1);
    }

    private static String tidyQuantity(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? "1 ud." : trimmed;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(new Locale("es", "ES"));
    }

    private static long uniqueId() {
        return System.currentTimeMillis() * 1000L + Math.abs(System.nanoTime() % 1000L);
    }
}
