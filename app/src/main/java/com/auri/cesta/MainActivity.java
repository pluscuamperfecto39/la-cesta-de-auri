package com.auri.cesta;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_IMPORT_LIST = 405;
    private static final int REQUEST_TAKE_PRODUCT_PHOTO = 406;
    private static final String STATE_PHOTO_ITEM_ID = "photo_item_id";
    private static final String STATE_PHOTO_PATH = "photo_path";
    private static final int PURPLE = Color.rgb(104, 52, 153);
    private static final int PURPLE_DARK = Color.rgb(62, 24, 92);
    private static final int CREAM = Color.rgb(232, 245, 251);
    private static final int SURFACE = Color.rgb(248, 253, 255);
    private static final int NAV_BLUE = Color.rgb(211, 235, 246);
    private static final int LAVENDER = Color.rgb(218, 190, 237);
    private static final int PURPLE_MID = Color.rgb(112, 71, 143);
    private static final int VIOLET = Color.rgb(146, 86, 190);
    private static final int PURPLE_TINT = Color.rgb(243, 234, 249);
    private static final int INK = Color.rgb(52, 38, 61);
    private static final int MUTED = Color.rgb(122, 105, 130);
    private static final int LINE = Color.rgb(204, 224, 234);
    private static final int WHITE = Color.WHITE;

    private static final String[] CATEGORIES = {
            "Fruta y verdura", "Despensa", "Lácteos", "Panadería",
            "Carne y pescado", "Bebidas", "Hogar", "Higiene", "Otros"
    };

    private DataStore store;
    private FrameLayout content;
    private final TextView[] navItems = new TextView[3];
    private int currentTab;
    private long pendingPhotoItemId = -1L;
    private File pendingPhotoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            pendingPhotoItemId = savedInstanceState.getLong(STATE_PHOTO_ITEM_ID, -1L);
            String pendingPath = savedInstanceState.getString(STATE_PHOTO_PATH, "");
            if (!pendingPath.isEmpty()) pendingPhotoFile = new File(pendingPath);
        }
        store = new DataStore(this);
        ReminderReceiver.createChannel(this);
        getWindow().setStatusBarColor(CREAM);
        getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildShell();
        showTab(getIntent().getIntExtra("tab", 0));
        handleIncomingList(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showTab(intent.getIntExtra("tab", currentTab));
        handleIncomingList(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_PHOTO_ITEM_ID, pendingPhotoItemId);
        if (pendingPhotoFile != null) outState.putString(STATE_PHOTO_PATH, pendingPhotoFile.getAbsolutePath());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PRODUCT_PHOTO) {
            finishProductPhoto(resultCode, data);
        } else if (requestCode == REQUEST_IMPORT_LIST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importListFromUri(data.getData());
        }
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CREAM);
        root.setPadding(0, dp(8), 0, dp(10));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int safeTop = insets.getSystemWindowInsetTop();
            int safeBottom = insets.getSystemWindowInsetBottom();
            view.setPadding(0, safeTop + dp(8), 0, safeBottom + dp(10));
            return insets;
        });

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(11), dp(18), dp(10));

        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.auri_queen_cutout);
        mark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        mark.setContentDescription(getString(R.string.character_description));
        header.addView(mark, new LinearLayout.LayoutParams(dp(66), dp(70)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(8), 0, 0, 0);
        TextView name = text("La Cesta de Auri", 23, Gravity.START, PURPLE, false);
        name.setTypeface(Typeface.create("cursive", Typeface.BOLD_ITALIC));
        name.setLetterSpacing(0.01f);
        brand.addView(name);
        TextView subtitle = text("Compra con calma, recuerda con cariño", 12, Gravity.START, MUTED, false);
        subtitle.setMaxLines(2);
        brand.addView(subtitle);
        header.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.setMinimumHeight(dp(90));
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(6), dp(8), dp(7));
        nav.setBackgroundColor(WHITE);
        nav.setElevation(dp(12));
        String[] labels = {"🧺\nMi cesta", "★\nFrecuentes", "◷\nPara luego"};
        for (int i = 0; i < labels.length; i++) {
            final int tab = i;
            TextView item = text(labels[i], 14, Gravity.CENTER, PURPLE_DARK, true);
            item.setLines(2);
            item.setPadding(dp(5), dp(7), dp(5), dp(7));
            item.setOnClickListener(view -> showTab(tab));
            navItems[i] = item;
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(68), 1));
        }
        nav.setBackgroundColor(NAV_BLUE);
        root.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(86)));
        setContentView(root);
        root.requestApplyInsets();
    }

    private void showTab(int tab) {
        View focused = getCurrentFocus();
        if (focused != null) {
            hideKeyboard(focused);
            focused.clearFocus();
        }
        currentTab = Math.max(0, Math.min(2, tab));
        for (int i = 0; i < navItems.length; i++) {
            boolean selected = i == currentTab;
            navItems[i].setTextColor(selected ? WHITE : PURPLE_DARK);
            navItems[i].setBackground(selected ? round(PURPLE, 16) : null);
        }
        content.removeAllViews();
        if (currentTab == 0) renderHome();
        else if (currentTab == 1) renderFrequent();
        else renderFuture();
    }

    private void renderHome() {
        List<DataStore.BasketItem> items = store.getBasket();
        int purchased = 0;
        for (DataStore.BasketItem item : items) if (item.checked) purchased++;
        int pending = items.size() - purchased;

        ScrollView scroll = pageScroll();
        LinearLayout body = pageBody();
        scroll.addView(body);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(19), dp(20), dp(18));
        hero.setBackground(gradient(PURPLE, PURPLE_DARK, 24));
        TextView kicker = text("CESTA ACTUAL", 11, Gravity.START, LAVENDER, true);
        kicker.setLetterSpacing(0.12f);
        hero.addView(kicker);
        hero.addView(text(pending == 0 ? "Todo en orden" : pending + (pending == 1 ? " cosa pendiente" : " cosas pendientes"),
                26, Gravity.START, WHITE, true), marginTop(4));
        hero.addView(text(items.isEmpty() ? "Añade el primer producto y empieza tu compra." :
                purchased + " de " + items.size() + " productos en la cesta", 14, Gravity.START, Color.rgb(235, 219, 232), false), marginTop(5));

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(Math.max(1, items.size()));
        progress.setProgress(purchased);
        progress.setProgressTintList(ColorStateList.valueOf(LAVENDER));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(130, 85, 155)));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        progressParams.topMargin = dp(15);
        hero.addView(progress, progressParams);
        body.addView(hero, cardMargins(0, 0, 0, 14));

        LinearLayout composer = card();
        composer.setOrientation(LinearLayout.VERTICAL);
        composer.setPadding(dp(16), dp(15), dp(16), dp(15));
        LinearLayout composerTitle = new LinearLayout(this);
        composerTitle.setOrientation(LinearLayout.HORIZONTAL);
        composerTitle.setGravity(Gravity.CENTER_VERTICAL);
        composerTitle.addView(text("¿Qué ponemos en la cesta?", 17, Gravity.START, INK, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView importButton = pill("↓  Importar", PURPLE, NAV_BLUE);
        importButton.setOnClickListener(view -> openImportPicker());
        composerTitle.addView(importButton);
        composer.addView(composerTitle);

        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setGravity(Gravity.CENTER_VERTICAL);
        addRow.setPadding(0, dp(9), 0, 0);
        EditText product = input("Ej. Tomates, arroz, jabón…");
        product.setImeOptions(EditorInfo.IME_ACTION_DONE);
        addRow.addView(product, new LinearLayout.LayoutParams(0, dp(56), 1));
        Button add = primaryButton("+  Añadir");
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(dp(136), dp(56));
        addParams.leftMargin = dp(8);
        addRow.addView(add, addParams);
        composer.addView(addRow);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.HORIZONTAL);
        details.setPadding(0, dp(8), 0, 0);
        Spinner category = categorySpinner();
        details.addView(category, new LinearLayout.LayoutParams(0, dp(50), 1));
        EditText quantity = input("1 ud.");
        quantity.setText(getString(R.string.default_quantity));
        quantity.setSelectAllOnFocus(true);
        LinearLayout.LayoutParams quantityParams = new LinearLayout.LayoutParams(dp(104), dp(50));
        quantityParams.leftMargin = dp(8);
        details.addView(quantity, quantityParams);
        composer.addView(details);

        View.OnClickListener addAction = view -> {
            String value = product.getText().toString().trim();
            if (value.isEmpty()) {
                product.setError("Escribe un producto");
                product.requestFocus();
                return;
            }
            store.addBasketItem(value, category.getSelectedItem().toString(), quantity.getText().toString());
            hideKeyboard(product);
            Toast.makeText(this, "Añadido a tu cesta", Toast.LENGTH_SHORT).show();
            renderHomeInPlace();
        };
        add.setOnClickListener(addAction);
        product.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addAction.onClick(view);
                return true;
            }
            return false;
        });
        body.addView(composer, cardMargins(0, 0, 0, 19));

        if (!items.isEmpty()) {
            LinearLayout listHeader = new LinearLayout(this);
            listHeader.setOrientation(LinearLayout.HORIZONTAL);
            listHeader.setGravity(Gravity.CENTER_VERTICAL);
            listHeader.addView(sectionTitle("Tu lista"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView share = pill("Compartir archivo", WHITE, PURPLE);
            share.setOnClickListener(view -> shareBasketFile(items));
            listHeader.addView(share);
            body.addView(listHeader, cardMargins(0, 0, 0, 8));

            for (DataStore.BasketItem item : items) {
                if (!item.checked) body.addView(basketRow(item), cardMargins(0, 0, 0, 8));
            }
            if (purchased > 0) {
                body.addView(sectionTitle("Ya está en la cesta  ·  " + purchased), cardMargins(0, 12, 0, 8));
                for (DataStore.BasketItem item : items) {
                    if (item.checked) body.addView(basketRow(item), cardMargins(0, 0, 0, 8));
                }
                TextView clear = pill("Limpiar productos comprados", PURPLE, PURPLE_TINT);
                clear.setGravity(Gravity.CENTER);
                clear.setOnClickListener(view -> confirmClearPurchased());
                body.addView(clear, cardMargins(0, 7, 0, 8));
            }
        } else {
            body.addView(emptyState("✦", "Tu cesta espera ideas", "Añade arriba todo lo que necesites. Podrás ir tachándolo mientras compras."),
                    cardMargins(0, 2, 0, 12));
        }

        content.addView(scroll);
    }

    private View basketRow(DataStore.BasketItem item) {
        LinearLayout row = card();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(11), dp(8), dp(8), dp(8));

        CheckBox checkBox = new CheckBox(this);
        checkBox.setButtonTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{PURPLE, Color.rgb(192, 179, 200)}
        ));
        checkBox.setChecked(item.checked);
        row.addView(checkBox, new LinearLayout.LayoutParams(dp(46), dp(52)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(item.name, 16, Gravity.START, item.checked ? MUTED : INK, true);
        if (item.checked) title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        copy.addView(title);
        copy.addView(text(item.quantity + "  ·  " + item.category, 12, Gravity.START, MUTED, false), marginTop(3));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        File photoFile = validProductPhoto(item.photoPath);
        View photoButton;
        if (photoFile != null) {
            ImageView photo = new ImageView(this);
            photo.setImageBitmap(loadThumbnail(photoFile, 64));
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photo.setBackground(round(PURPLE_TINT, 12));
            photo.setClipToOutline(true);
            photo.setContentDescription(getString(R.string.view_product_photo, item.name));
            photo.setOnClickListener(view -> showProductPhoto(item, photoFile));
            photoButton = photo;
        } else {
            TextView camera = text("📷", 20, Gravity.CENTER, PURPLE_DARK, false);
            camera.setBackground(round(NAV_BLUE, 13));
            camera.setContentDescription(getString(R.string.take_product_photo, item.name));
            camera.setOnClickListener(view -> takeProductPhoto(item));
            photoButton = camera;
        }
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(dp(50), dp(50));
        photoParams.leftMargin = dp(6);
        row.addView(photoButton, photoParams);

        TextView remove = text("×", 25, Gravity.CENTER, MUTED, false);
        remove.setContentDescription("Eliminar " + item.name);
        remove.setOnClickListener(view -> removeBasketItem(item.id));
        row.addView(remove, new LinearLayout.LayoutParams(dp(42), dp(48)));

        checkBox.setOnCheckedChangeListener((button, checked) -> {
            List<DataStore.BasketItem> all = store.getBasket();
            for (DataStore.BasketItem saved : all) {
                if (saved.id == item.id) {
                    saved.checked = checked;
                    if (checked && !saved.counted) {
                        saved.counted = true;
                        store.incrementFrequent(saved.name, saved.category);
                    }
                    break;
                }
            }
            store.saveBasket(all);
            if (checked) Toast.makeText(this, "¡Uno menos!", Toast.LENGTH_SHORT).show();
            renderHomeInPlace();
        });
        return row;
    }

    private void renderFrequent() {
        List<DataStore.FrequentItem> frequent = store.getFrequent();
        ScrollView scroll = pageScroll();
        LinearLayout body = pageBody();
        scroll.addView(body);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(19), dp(20), dp(19));
        hero.setBackground(gradient(LAVENDER, Color.rgb(188, 143, 219), 24));
        hero.addView(text("TUS IMPRESCINDIBLES", 11, Gravity.START, PURPLE_DARK, true));
        hero.addView(text("Lo de siempre, en un toque", 24, Gravity.START, PURPLE_DARK, true), marginTop(5));
        hero.addView(text("Cada producto que tachas aprende su lugar aquí.", 14, Gravity.START, PURPLE_MID, false), marginTop(6));
        body.addView(hero, cardMargins(0, 0, 0, 19));

        if (!frequent.isEmpty()) {
            body.addView(sectionTitle("Más repetidos"), cardMargins(0, 0, 0, 9));
            int position = 1;
            for (DataStore.FrequentItem item : frequent) {
                LinearLayout row = card();
                row.setPadding(dp(14), dp(11), dp(11), dp(11));
                row.setGravity(Gravity.CENTER_VERTICAL);
                TextView rank = text(String.valueOf(position++), 14, Gravity.CENTER, PURPLE, true);
                rank.setBackground(round(PURPLE_TINT, 16));
                row.addView(rank, new LinearLayout.LayoutParams(dp(34), dp(34)));
                LinearLayout copy = new LinearLayout(this);
                copy.setOrientation(LinearLayout.VERTICAL);
                copy.setPadding(dp(12), 0, dp(8), 0);
                copy.addView(text(item.name, 16, Gravity.START, INK, true));
                copy.addView(text("Comprado " + item.count + (item.count == 1 ? " vez" : " veces") + "  ·  " + item.category,
                        12, Gravity.START, MUTED, false), marginTop(3));
                row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                TextView add = pill("+ Añadir", WHITE, PURPLE);
                add.setOnClickListener(view -> {
                    store.addBasketItem(item.name, item.category, "1 ud.");
                    Toast.makeText(this, item.name + " añadido", Toast.LENGTH_SHORT).show();
                });
                row.addView(add);
                body.addView(row, cardMargins(0, 0, 0, 8));
            }
        } else {
            body.addView(emptyState("★", "Aquí aparecerán tus favoritos",
                    "Cuando taches productos de la cesta, guardaremos cuántas veces los compras."), cardMargins(0, 0, 0, 18));
        }

        body.addView(sectionTitle("Ideas rápidas"), cardMargins(0, 11, 0, 9));
        String[][] suggestions = {
                {"Leche", "Lácteos"}, {"Pan", "Panadería"},
                {"Huevos", "Despensa"}, {"Plátanos", "Fruta y verdura"},
                {"Papel de cocina", "Hogar"}, {"Agua", "Bebidas"}
        };
        for (int i = 0; i < suggestions.length; i += 2) {
            LinearLayout pair = new LinearLayout(this);
            pair.setOrientation(LinearLayout.HORIZONTAL);
            pair.addView(suggestionCard(suggestions[i][0], suggestions[i][1]), new LinearLayout.LayoutParams(0, dp(78), 1));
            SpaceView spacer = new SpaceView(this);
            pair.addView(spacer, new LinearLayout.LayoutParams(dp(8), 1));
            pair.addView(suggestionCard(suggestions[i + 1][0], suggestions[i + 1][1]), new LinearLayout.LayoutParams(0, dp(78), 1));
            body.addView(pair, cardMargins(0, 0, 0, 8));
        }
        content.addView(scroll);
    }

    private View suggestionCard(String name, String category) {
        LinearLayout card = card();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(10), dp(10), dp(10));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(name, 15, Gravity.START, INK, true));
        copy.addView(text(category, 11, Gravity.START, MUTED, false), marginTop(2));
        card.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView plus = text("+", 23, Gravity.CENTER, PURPLE, true);
        plus.setBackground(round(PURPLE_TINT, 14));
        card.addView(plus, new LinearLayout.LayoutParams(dp(34), dp(34)));
        card.setOnClickListener(view -> {
            store.addBasketItem(name, category, "1 ud.");
            Toast.makeText(this, name + " añadido", Toast.LENGTH_SHORT).show();
        });
        return card;
    }

    private void renderFuture() {
        List<DataStore.FutureItem> future = store.getFuture();
        ScrollView scroll = pageScroll();
        LinearLayout body = pageBody();
        scroll.addView(body);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(19), dp(20), dp(19));
        hero.setBackground(gradient(VIOLET, PURPLE, 24));
        hero.addView(text("TENGO QUE COMPRAR", 11, Gravity.START, LAVENDER, true));
        hero.addView(text("Que no se te escape nada", 24, Gravity.START, WHITE, true), marginTop(5));
        hero.addView(text("Guarda una compra futura y Auri te avisará.", 14, Gravity.START, Color.rgb(255, 237, 232), false), marginTop(6));
        Button plan = lightButton("+  Planear una compra");
        plan.setOnClickListener(view -> showFutureDialog());
        hero.addView(plan, sizedTop(ViewGroup.LayoutParams.MATCH_PARENT, 49, 16));
        body.addView(hero, cardMargins(0, 0, 0, 18));

        if (future.isEmpty()) {
            body.addView(emptyState("◷", "Tu lista de después está vacía",
                    "Perfecta para regalos, reposiciones o cualquier compra que pueda esperar."), cardMargins(0, 0, 0, 12));
        } else {
            body.addView(sectionTitle("Próximas compras  ·  " + future.size()), cardMargins(0, 0, 0, 9));
            for (DataStore.FutureItem item : future) {
                body.addView(futureRow(item), cardMargins(0, 0, 0, 9));
            }
        }
        content.addView(scroll);
    }

    private View futureRow(DataStore.FutureItem item) {
        boolean past = item.whenMillis < System.currentTimeMillis();
        LinearLayout card = card();
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(12), dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.TOP);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(item.title, 17, Gravity.START, INK, true));
        TextView date = text(past ? "Fecha pasada  ·  " + formatDate(item.whenMillis) : formatDate(item.whenMillis),
                13, Gravity.START, past ? VIOLET : PURPLE, true);
        copy.addView(date, marginTop(5));
        copy.addView(text(item.notify ? "🔔 Aviso activado" : "Aviso desactivado", 12, Gravity.START, MUTED, false), marginTop(4));
        top.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView remove = text("×", 24, Gravity.CENTER, MUTED, false);
        remove.setOnClickListener(view -> removeFuture(item.id));
        top.addView(remove, new LinearLayout.LayoutParams(dp(40), dp(40)));
        card.addView(top);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, dp(11), 0, 0);
        TextView toBasket = pill("Pasar a mi cesta", WHITE, PURPLE);
        toBasket.setOnClickListener(view -> {
            store.addBasketItem(item.title, "Otros", "1 ud.");
            removeFuture(item.id);
            Toast.makeText(this, "Pasado a tu cesta", Toast.LENGTH_SHORT).show();
        });
        actions.addView(toBasket);
        card.addView(actions);
        return card;
    }

    private void showFutureDialog() {
        Calendar chosen = Calendar.getInstance();
        chosen.add(Calendar.DAY_OF_MONTH, 1);
        chosen.set(Calendar.HOUR_OF_DAY, 10);
        chosen.set(Calendar.MINUTE, 0);
        chosen.set(Calendar.SECOND, 0);
        chosen.set(Calendar.MILLISECOND, 0);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(4), dp(22), dp(2));
        form.addView(text("¿Qué tendrás que comprar?", 13, Gravity.START, MUTED, true));
        EditText title = input("Ej. Regalo de cumpleaños");
        form.addView(title, sizedTop(ViewGroup.LayoutParams.MATCH_PARENT, 52, 7));

        LinearLayout dateTime = new LinearLayout(this);
        dateTime.setOrientation(LinearLayout.HORIZONTAL);
        dateTime.setPadding(0, dp(12), 0, 0);
        TextView dateButton = choiceButton(shortDate(chosen.getTimeInMillis()));
        TextView timeButton = choiceButton(shortTime(chosen.getTimeInMillis()));
        dateTime.addView(dateButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        timeParams.leftMargin = dp(8);
        dateTime.addView(timeButton, timeParams);
        form.addView(dateTime);

        Switch alarm = new Switch(this);
        alarm.setText(getString(R.string.notify_at_time));
        alarm.setTextColor(INK);
        alarm.setTextSize(14);
        alarm.setChecked(true);
        alarm.setButtonTintList(ColorStateList.valueOf(PURPLE));
        form.addView(alarm, sizedTop(ViewGroup.LayoutParams.MATCH_PARENT, 54, 8));
        form.addView(text("El aviso funciona aunque cierres la aplicación.", 12, Gravity.START, MUTED, false));

        dateButton.setOnClickListener(view -> {
            DatePickerDialog picker = new DatePickerDialog(this, (DatePicker v, int year, int month, int day) -> {
                chosen.set(Calendar.YEAR, year);
                chosen.set(Calendar.MONTH, month);
                chosen.set(Calendar.DAY_OF_MONTH, day);
                dateButton.setText(shortDate(chosen.getTimeInMillis()));
            }, chosen.get(Calendar.YEAR), chosen.get(Calendar.MONTH), chosen.get(Calendar.DAY_OF_MONTH));
            picker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            picker.show();
        });
        timeButton.setOnClickListener(view -> new TimePickerDialog(this, (TimePicker v, int hour, int minute) -> {
            chosen.set(Calendar.HOUR_OF_DAY, hour);
            chosen.set(Calendar.MINUTE, minute);
            timeButton.setText(shortTime(chosen.getTimeInMillis()));
        }, chosen.get(Calendar.HOUR_OF_DAY), chosen.get(Calendar.MINUTE), true).show());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Planear una compra")
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(PURPLE);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(MUTED);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String value = title.getText().toString().trim();
                if (value.isEmpty()) {
                    title.setError("Escribe qué necesitas comprar");
                    return;
                }
                if (chosen.getTimeInMillis() <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Elige una fecha y hora futuras", Toast.LENGTH_SHORT).show();
                    return;
                }
                DataStore.FutureItem item = store.addFutureItem(value, chosen.getTimeInMillis(), alarm.isChecked());
                if (item.notify) {
                    askNotificationPermissionIfNeeded();
                    ReminderScheduler.schedule(this, item);
                }
                dialog.dismiss();
                Toast.makeText(this, item.notify ? "Compra guardada con aviso" : "Compra guardada", Toast.LENGTH_SHORT).show();
                showTab(2);
            });
        });
        dialog.getWindow();
        dialog.show();
    }

    private void askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 402);
        }
    }

    private void removeBasketItem(long id) {
        List<DataStore.BasketItem> all = store.getBasket();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).id == id) {
                deleteProductPhoto(all.get(i).photoPath);
                all.remove(i);
            }
        }
        store.saveBasket(all);
        renderHomeInPlace();
    }

    private void confirmClearPurchased() {
        new AlertDialog.Builder(this)
                .setTitle("¿Limpiar lo comprado?")
                .setMessage("Se quitarán de esta cesta, pero seguirán contando entre tus productos frecuentes.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Limpiar", (dialog, which) -> {
                    List<DataStore.BasketItem> all = store.getBasket();
                    for (int i = all.size() - 1; i >= 0; i--) {
                        if (all.get(i).checked) {
                            deleteProductPhoto(all.get(i).photoPath);
                            all.remove(i);
                        }
                    }
                    store.saveBasket(all);
                    renderHomeInPlace();
                })
                .show();
    }

    private void removeFuture(long id) {
        ReminderScheduler.cancel(this, id);
        List<DataStore.FutureItem> all = store.getFuture();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).id == id) all.remove(i);
        }
        store.saveFuture(all);
        showTab(2);
    }

    private void renderHomeInPlace() {
        content.removeAllViews();
        renderHome();
    }

    private void shareBasketFile(List<DataStore.BasketItem> items) {
        StringBuilder message = new StringBuilder("🧺 LA CESTA DE AURI\n\n");
        JSONObject document = new JSONObject();
        JSONArray products = new JSONArray();
        for (DataStore.BasketItem item : items) {
            message.append(item.checked ? "✓ " : "□ ")
                    .append(item.name)
                    .append(" — ")
                    .append(item.quantity)
                    .append('\n');
            JSONObject product = new JSONObject();
            try {
                product.put("name", item.name);
                product.put("category", item.category);
                product.put("quantity", item.quantity);
                product.put("checked", item.checked);
                products.put(product);
            } catch (Exception ignored) {
            }
        }
        message.append("\nAbre el archivo .auri con La Cesta de Auri en Android o iPhone.");
        try {
            document.put("format", "la-cesta-de-auri");
            document.put("version", 1);
            document.put("createdAt", System.currentTimeMillis());
            document.put("items", products);

            File directory = new File(getCacheDir(), "shared_lists");
            if (!directory.exists() && !directory.mkdirs()) throw new IOException("No se pudo preparar el archivo");
            String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(new Date());
            File file = new File(directory, "Cesta-de-Auri-" + stamp + ".auri");
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(document.toString(2).getBytes(StandardCharsets.UTF_8));
            }

            Uri uri = ShareListProvider.uriForFile(this, file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType(ShareListProvider.MIME_TYPE);
            share.putExtra(Intent.EXTRA_SUBJECT, "Lista compatible de La Cesta de Auri");
            share.putExtra(Intent.EXTRA_TEXT, message.toString());
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.setClipData(ClipData.newUri(getContentResolver(), "Lista de la compra", uri));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Compartir con Android o iPhone"));
        } catch (Exception exception) {
            Toast.makeText(this, "No se pudo crear el archivo de la lista", Toast.LENGTH_LONG).show();
        }
    }

    private void openImportPicker() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        try {
            startActivityForResult(picker, REQUEST_IMPORT_LIST);
        } catch (Exception exception) {
            Toast.makeText(this, "No se encontró un gestor de archivos", Toast.LENGTH_LONG).show();
        }
    }

    private void takeProductPhoto(DataStore.BasketItem item) {
        File directory = new File(getFilesDir(), "product_photos");
        try {
            if (!directory.exists() && !directory.mkdirs()) throw new IOException("No se pudo crear la carpeta");
            File photo = new File(directory, "product_" + Math.abs(item.id) + "_" + System.currentTimeMillis() + ".jpg");
            if (!photo.createNewFile()) throw new IOException("No se pudo crear la foto");

            Uri uri = ProductPhotoProvider.uriForFile(this, photo);
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            camera.setClipData(ClipData.newRawUri("Foto del producto", uri));
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            pendingPhotoItemId = item.id;
            pendingPhotoFile = photo;
            startActivityForResult(camera, REQUEST_TAKE_PRODUCT_PHOTO);
        } catch (Exception exception) {
            if (pendingPhotoFile != null) pendingPhotoFile.delete();
            pendingPhotoItemId = -1L;
            pendingPhotoFile = null;
            Toast.makeText(this, "No se pudo abrir la cámara del móvil", Toast.LENGTH_LONG).show();
        }
    }

    private void showProductPhoto(DataStore.BasketItem item, File photoFile) {
        Bitmap previewBitmap = loadThumbnail(
                photoFile,
                Math.max(320, getResources().getConfiguration().screenWidthDp - 48)
        );
        if (previewBitmap == null) {
            Toast.makeText(this, "No se pudo abrir la foto", Toast.LENGTH_LONG).show();
            return;
        }

        ImageView preview = new ImageView(this);
        preview.setImageBitmap(previewBitmap);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setAdjustViewBounds(true);
        preview.setMinimumHeight(dp(220));
        preview.setMaxHeight(dp(560));
        preview.setBackground(round(Color.rgb(28, 21, 33), 18));
        preview.setClipToOutline(true);
        preview.setContentDescription(getString(R.string.product_photo_preview, item.name));

        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(dp(18), dp(4), dp(18), 0);
        holder.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        new AlertDialog.Builder(this)
                .setTitle(item.name)
                .setView(holder)
                .setNegativeButton(R.string.close_photo, null)
                .setPositiveButton(R.string.change_product_photo, (dialog, which) -> takeProductPhoto(item))
                .show();
    }

    private void finishProductPhoto(int resultCode, Intent data) {
        File captured = pendingPhotoFile;
        long itemId = pendingPhotoItemId;
        pendingPhotoFile = null;
        pendingPhotoItemId = -1L;

        if (resultCode == RESULT_OK && captured != null) {
            if (captured.length() == 0 && data != null && data.getExtras() != null) {
                Object preview = data.getExtras().get("data");
                if (preview instanceof Bitmap) {
                    try (FileOutputStream output = new FileOutputStream(captured)) {
                        ((Bitmap) preview).compress(Bitmap.CompressFormat.JPEG, 92, output);
                    } catch (IOException ignored) {
                    }
                }
            }
            if (captured.length() > 0) {
                List<DataStore.BasketItem> all = store.getBasket();
                for (DataStore.BasketItem saved : all) {
                    if (saved.id == itemId) {
                        String previousPhoto = saved.photoPath;
                        saved.photoPath = captured.getAbsolutePath();
                        store.saveBasket(all);
                        if (previousPhoto != null && !previousPhoto.equals(saved.photoPath)) {
                            deleteProductPhoto(previousPhoto);
                        }
                        Toast.makeText(this, "Foto guardada en la cesta", Toast.LENGTH_SHORT).show();
                        renderHomeInPlace();
                        return;
                    }
                }
            }
        }

        if (captured != null) captured.delete();
        if (resultCode == RESULT_OK) {
            Toast.makeText(this, "No se pudo guardar la foto", Toast.LENGTH_LONG).show();
        }
        renderHomeInPlace();
    }

    private File validProductPhoto(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        try {
            File base = new File(getFilesDir(), "product_photos").getCanonicalFile();
            File photo = new File(path).getCanonicalFile();
            if (photo.getPath().startsWith(base.getPath() + File.separator) && photo.isFile() && photo.length() > 0) {
                return photo;
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private void deleteProductPhoto(String path) {
        File photo = validProductPhoto(path);
        if (photo != null) photo.delete();
    }

    private Bitmap loadThumbnail(File file, int targetDp) {
        int targetPixels = dp(targetDp);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        int largestSide = Math.max(bounds.outWidth, bounds.outHeight);
        while (largestSide / (options.inSampleSize * 2) >= targetPixels) {
            options.inSampleSize *= 2;
        }
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    private void handleIncomingList(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            importListFromUri(intent.getData());
            intent.setAction(null);
        }
    }

    private void importListFromUri(Uri uri) {
        try {
            String json = readSmallTextFile(uri);
            JSONObject document = new JSONObject(json);
            if (!"la-cesta-de-auri".equals(document.optString("format")) || document.optInt("version") != 1) {
                throw new IOException("Formato desconocido");
            }
            JSONArray array = document.optJSONArray("items");
            if (array == null || array.length() == 0 || array.length() > 200) {
                throw new IOException("La lista está vacía o es demasiado grande");
            }

            List<DataStore.BasketItem> imported = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                String name = shorten(object.optString("name").trim(), 100);
                if (name.isEmpty()) continue;
                String category = safeCategory(object.optString("category"));
                String quantity = shorten(object.optString("quantity", "1 ud.").trim(), 40);
                imported.add(new DataStore.BasketItem(0, name, category, quantity));
            }
            if (imported.isEmpty()) throw new IOException("No hay productos válidos");
            showImportConfirmation(imported);
        } catch (Exception exception) {
            new AlertDialog.Builder(this)
                    .setTitle("No se pudo importar")
                    .setMessage("El archivo no es una lista válida de La Cesta de Auri.")
                    .setPositiveButton("Aceptar", null)
                    .show();
        }
    }

    private void showImportConfirmation(List<DataStore.BasketItem> imported) {
        StringBuilder preview = new StringBuilder();
        int shown = Math.min(4, imported.size());
        for (int i = 0; i < shown; i++) preview.append("\n• ").append(imported.get(i).name);
        if (imported.size() > shown) preview.append("\n• …y ").append(imported.size() - shown).append(" más");
        new AlertDialog.Builder(this)
                .setTitle("Importar lista de Android o iPhone")
                .setMessage("Se añadirán " + imported.size() + (imported.size() == 1 ? " producto" : " productos") +
                        " a tu cesta:" + preview)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Importar", (dialog, which) -> {
                    store.importBasketItems(imported);
                    showTab(0);
                    Toast.makeText(this, "Lista importada correctamente", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private String readSmallTextFile(Uri uri) throws IOException {
        StringBuilder result = new StringBuilder();
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IOException("Archivo no disponible");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    result.append(buffer, 0, read);
                    if (result.length() > 524288) throw new IOException("Archivo demasiado grande");
                }
            }
        }
        return result.toString();
    }

    private String safeCategory(String value) {
        for (String category : CATEGORIES) if (category.equals(value)) return category;
        return "Otros";
    }

    private String shorten(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private ScrollView pageScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        return scroll;
    }

    private LinearLayout pageBody() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(17), dp(7), dp(17), dp(30));
        return body;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackground(round(SURFACE, 18));
        card.setElevation(dp(1));
        return card;
    }

    private View emptyState(String symbol, String title, String description) {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(24), dp(23), dp(24), dp(23));
        empty.setBackground(outline(SURFACE, LINE, 18));
        TextView mark = text(symbol, 25, Gravity.CENTER, PURPLE, true);
        mark.setBackground(round(PURPLE_TINT, 22));
        empty.addView(mark, new LinearLayout.LayoutParams(dp(50), dp(50)));
        empty.addView(text(title, 17, Gravity.CENTER, INK, true), marginTop(11));
        empty.addView(text(description, 13, Gravity.CENTER, MUTED, false), marginTop(6));
        return empty;
    }

    private TextView sectionTitle(String value) {
        return text(value, 18, Gravity.START, INK, true);
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(Color.rgb(166, 153, 160));
        input.setTextColor(INK);
        input.setTextSize(14);
        input.setSingleLine(true);
        input.setPadding(dp(13), 0, dp(13), 0);
        input.setBackground(outline(SURFACE, LINE, 13));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        return input;
    }

    private Spinner categorySpinner() {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CATEGORIES) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(INK);
                view.setTextSize(13);
                view.setPadding(dp(12), 0, dp(8), 0);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setBackground(outline(SURFACE, LINE, 13));
        return spinner;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(round(PURPLE_DARK, 15));
        button.setElevation(dp(2));
        return button;
    }

    private Button lightButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(PURPLE);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(round(SURFACE, 14));
        return button;
    }

    private TextView choiceButton(String label) {
        TextView button = text(label, 14, Gravity.CENTER, PURPLE, true);
        button.setBackground(outline(SURFACE, LINE, 13));
        return button;
    }

    private TextView pill(String label, int foreground, int background) {
        TextView pill = text(label, 12, Gravity.CENTER, foreground, true);
        pill.setPadding(dp(13), dp(8), dp(13), dp(8));
        pill.setBackground(round(background, 15));
        return pill;
    }

    private TextView text(String value, float size, int gravity, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(gravity);
        text.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        text.setLineSpacing(0, 1.08f);
        return text;
    }

    private GradientDrawable round(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private GradientDrawable outline(int color, int stroke, float radius) {
        GradientDrawable drawable = round(color, radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable gradient(int start, int end, float radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout.LayoutParams marginTop(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(top);
        return params;
    }

    private LinearLayout.LayoutParams cardMargins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private LinearLayout.LayoutParams sizedTop(int width, int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, dp(height));
        params.topMargin = dp(top);
        return params;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatDate(long millis) {
        String value = new SimpleDateFormat("EEEE, d 'de' MMM · HH:mm", new Locale("es", "ES")).format(new Date(millis));
        return value.substring(0, 1).toUpperCase(new Locale("es", "ES")) + value.substring(1);
    }

    private String shortDate(long millis) {
        return "📅  " + new SimpleDateFormat("dd/MM/yyyy", new Locale("es", "ES")).format(new Date(millis));
    }

    private String shortTime(long millis) {
        return "◷  " + new SimpleDateFormat("HH:mm", new Locale("es", "ES")).format(new Date(millis));
    }

    private void hideKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /** Un separador mínimo sin traer dependencias externas. */
    private static final class SpaceView extends View {
        SpaceView(Context context) {
            super(context);
        }
    }
}
