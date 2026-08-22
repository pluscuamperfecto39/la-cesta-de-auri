package com.auri.cesta;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Proporciona las filas desplazables de la cesta al widget. */
public final class BasketWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new BasketFactory(getApplicationContext());
    }

    private static final class BasketFactory implements RemoteViewsService.RemoteViewsFactory {
        private final Context context;
        private final List<DataStore.BasketItem> items = new ArrayList<>();

        BasketFactory(Context context) {
            this.context = context;
        }

        @Override
        public void onCreate() {
            reload();
        }

        @Override
        public void onDataSetChanged() {
            reload();
        }

        private synchronized void reload() {
            items.clear();
            items.addAll(new DataStore(context).getBasket());
        }

        @Override
        public void onDestroy() {
            synchronized (this) {
                items.clear();
            }
        }

        @Override
        public synchronized int getCount() {
            return items.size();
        }

        @Override
        public synchronized RemoteViews getViewAt(int position) {
            if (position < 0 || position >= items.size()) return null;
            DataStore.BasketItem item = items.get(position);
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_basket_item);

            row.setTextViewText(
                    R.id.widget_item_check,
                    context.getString(item.checked ? R.string.widget_checked : R.string.widget_unchecked)
            );
            row.setTextColor(
                    R.id.widget_item_check,
                    context.getColor(item.checked ? R.color.auri_plum : R.color.auri_muted)
            );

            CharSequence title = item.name;
            if (item.checked) {
                SpannableString crossed = new SpannableString(item.name);
                crossed.setSpan(new StrikethroughSpan(), 0, crossed.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                title = crossed;
            }
            row.setTextViewText(R.id.widget_item_name, title);
            row.setTextColor(
                    R.id.widget_item_name,
                    context.getColor(item.checked ? R.color.auri_muted : R.color.auri_ink)
            );
            row.setTextViewText(R.id.widget_item_detail, item.quantity + " · " + item.category);
            Bitmap photo = loadPhoto(item.photoPath);
            if (photo != null) {
                row.setViewVisibility(R.id.widget_item_photo, View.VISIBLE);
                row.setImageViewBitmap(R.id.widget_item_photo, photo);
            } else {
                row.setViewVisibility(R.id.widget_item_photo, View.GONE);
            }
            row.setInt(
                    R.id.widget_item_root,
                    "setBackgroundResource",
                    item.checked ? R.drawable.widget_row_purchased : R.drawable.widget_row_background
            );

            Intent fillIn = new Intent();
            fillIn.putExtra("tab", 0);
            row.setOnClickFillInIntent(R.id.widget_item_root, fillIn);
            row.setOnClickFillInIntent(R.id.widget_item_check, fillIn);
            row.setOnClickFillInIntent(R.id.widget_item_photo, fillIn);
            row.setOnClickFillInIntent(R.id.widget_item_name, fillIn);
            row.setOnClickFillInIntent(R.id.widget_item_detail, fillIn);
            return row;
        }

        private Bitmap loadPhoto(String path) {
            if (path == null || path.isEmpty()) return null;
            File file = new File(path);
            if (!file.isFile() || file.length() == 0) return null;

            int targetPixels = Math.round(48 * context.getResources().getDisplayMetrics().density);
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

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public synchronized long getItemId(int position) {
            return position >= 0 && position < items.size() ? items.get(position).id : position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
