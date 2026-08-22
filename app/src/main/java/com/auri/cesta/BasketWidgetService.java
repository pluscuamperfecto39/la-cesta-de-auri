package com.auri.cesta;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

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
            row.setInt(
                    R.id.widget_item_root,
                    "setBackgroundResource",
                    item.checked ? R.drawable.widget_row_purchased : R.drawable.widget_row_background
            );

            Intent fillIn = new Intent();
            fillIn.putExtra("tab", 0);
            row.setOnClickFillInIntent(R.id.widget_item_root, fillIn);
            row.setOnClickFillInIntent(R.id.widget_item_check, fillIn);
            row.setOnClickFillInIntent(R.id.widget_item_name, fillIn);
            row.setOnClickFillInIntent(R.id.widget_item_detail, fillIn);
            return row;
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
