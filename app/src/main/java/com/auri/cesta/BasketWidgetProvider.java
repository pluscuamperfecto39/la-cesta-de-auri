package com.auri.cesta;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

/** Widget de la pantalla de inicio que refleja la cesta guardada en el teléfono. */
public final class BasketWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_OPEN_FROM_WIDGET = "com.auri.cesta.OPEN_FROM_WIDGET";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_OPEN_FROM_WIDGET.equals(intent.getAction())) {
            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.putExtra("tab", 0);
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(openIntent);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            manager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId));
        }
        manager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
    }

    @Override
    public void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager manager,
            int appWidgetId,
            android.os.Bundle newOptions
    ) {
        manager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId));
        manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);
    }

    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, BasketWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids.length == 0) return;

        BasketWidgetProvider provider = new BasketWidgetProvider();
        provider.onUpdate(context.getApplicationContext(), manager, ids);
    }

    private static RemoteViews buildViews(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_basket);
        List<DataStore.BasketItem> items = new DataStore(context).getBasket();

        int pending = 0;
        for (DataStore.BasketItem item : items) if (!item.checked) pending++;
        String count = context.getResources().getQuantityString(
                R.plurals.widget_pending_count,
                pending,
                pending
        );
        views.setTextViewText(R.id.widget_count, count);
        views.setTextViewText(
                R.id.widget_empty,
                context.getString(items.isEmpty() ? R.string.widget_empty : R.string.widget_all_bought)
        );

        Intent serviceIntent = new Intent(context, BasketWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_list, serviceIntent);
        views.setEmptyView(R.id.widget_list, R.id.widget_empty);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra("tab", 0);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openApp = PendingIntent.getActivity(
                context,
                appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, openApp);
        views.setOnClickPendingIntent(R.id.widget_header, openApp);
        views.setOnClickPendingIntent(R.id.widget_empty, openApp);
        views.setOnClickPendingIntent(R.id.widget_open, openApp);

        int templateFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            templateFlags |= PendingIntent.FLAG_MUTABLE;
        }
        Intent templateIntent = new Intent(context, BasketWidgetProvider.class);
        templateIntent.setAction(ACTION_OPEN_FROM_WIDGET);
        templateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        templateIntent.setData(Uri.parse(templateIntent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent itemTemplate = PendingIntent.getBroadcast(
                context,
                10000 + appWidgetId,
                templateIntent,
                templateFlags
        );
        views.setPendingIntentTemplate(R.id.widget_list, itemTemplate);
        views.setViewVisibility(R.id.widget_open, View.VISIBLE);
        return views;
    }
}
