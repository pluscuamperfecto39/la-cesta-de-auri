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
        public String photoPath;
        public boolean checked;
        public boolean counted;

        public BasketItem(long id, String name, String category, String quantity) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.quantity = quantity;
            this.photoPath = "";
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
                item.photoPath = object.optString("photoPath", "");
                result.add(item);
            }
        } catch (JSONException ignored) {
            // Si un dato quedara incompleto, la app sigue funcionando con una cesta vacía.
        }
        return result;
    }

    public void saveBasket(List<BasketItem> items) {
        prefs.edit().putString(KEY_BASKET, basketJson(items).toString()).apply();
        BasketWidgetProvider.refreshAll(appContext);
    }

    private JSONArray basketJson(List<BasketItem> items) {
        JSONArray array = new JSONArray();
        for (BasketItem item : items) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", item.id);
                object.put("name", item.name);
                object.put("category", item.category);
                object.put("quantity", item.quantity);
                object.put("photoPath", item.photoPath == null ? "" : item.photoPath);
                object.put("checked", item.checked);
                object.put("counted", item.counted);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        return array;
    }

    public void addBasketItem(String name, String category, String quantity) {
        List<BasketItem> items = getBasket();
        items.add(0, new BasketItem(uniqueId(), tidy(name), category, tidyQuantity(quantity)));
        saveBasket(items);
    }

    /** Añade varios productos de una vez manteniendo el orden en el que se escribieron. */
    public void addBasketItems(List<String> names, String category, String quantity) {
        List<BasketItem> items = getBasket();
        for (int i = names.size() - 1; i >= 0; i--) {
            String name = tidy(names.get(i));
            if (!name.isEmpty()) {
                items.add(0, new BasketItem(uniqueId(), name, category, tidyQuantity(quantity)));
            }
        }
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
        prefs.edit().putString(KEY_FUTURE, futureJson(items).toString()).apply();
    }

    private JSONArray futureJson(List<FutureItem> items) {
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
        return array;
    }

    public FutureItem addFutureItem(String title, long whenMillis, boolean notify) {
        List<FutureItem> items = getFuture();
        FutureItem item = new FutureItem(uniqueId(), tidy(title), whenMillis, notify);
        items.add(item);
        saveFuture(items);
        return item;
    }

    /**
     * Traslada de una vez las compras cuya fecha ya ha llegado. La cesta y la lista
     * futura se escriben en la misma operación para evitar que un artículo aparezca
     * en las dos listas si el proceso se interrumpe.
     */
    public List<FutureItem> moveDueFutureItems(long nowMillis) {
        List<FutureItem> future = getFuture();
        List<FutureItem> moved = new ArrayList<>();
        for (FutureItem item : future) {
            if (item.whenMillis <= nowMillis) moved.add(item);
        }
        if (moved.isEmpty()) return moved;

        List<BasketItem> basket = getBasket();
        for (FutureItem item : moved) {
            basket.add(0, new BasketItem(item.id, item.title, "Otros", "1 ud."));
        }
        future.removeIf(item -> item.whenMillis <= nowMillis);
        saveBasketAndFuture(basket, future);
        return moved;
    }

    /** Traslada manualmente una compra futura y devuelve el elemento trasladado. */
    public FutureItem moveFutureToBasket(long id) {
        List<FutureItem> future = getFuture();
        FutureItem moved = null;
        for (FutureItem item : future) {
            if (item.id == id) {
                moved = item;
                break;
            }
        }
        if (moved == null) return null;

        List<BasketItem> basket = getBasket();
        basket.add(0, new BasketItem(moved.id, moved.title, "Otros", "1 ud."));
        FutureItem selected = moved;
        future.removeIf(item -> item.id == selected.id);
        saveBasketAndFuture(basket, future);
        return moved;
    }

    private void saveBasketAndFuture(List<BasketItem> basket, List<FutureItem> future) {
        prefs.edit()
                .putString(KEY_BASKET, basketJson(basket).toString())
                .putString(KEY_FUTURE, futureJson(future).toString())
                .apply();
        BasketWidgetProvider.refreshAll(appContext);
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
