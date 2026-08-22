package com.auri.cesta;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Programa avisos incluso con la app cerrada. */
public final class ReminderScheduler {
    private ReminderScheduler() {
    }

    public static void schedule(Context context, DataStore.FutureItem item) {
        if (!item.notify || item.whenMillis <= System.currentTimeMillis()) return;

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = reminderIntent(context, item);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.whenMillis, pendingIntent);
        } else {
            // Android puede pedir permiso adicional para alarmas exactas. El aviso sigue
            // programado aunque el sistema decida retrasarlo unos minutos para ahorrar batería.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.whenMillis, pendingIntent);
        }
    }

    public static void cancel(Context context, long id) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(id),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (manager != null && pendingIntent != null) {
            manager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static PendingIntent reminderIntent(Context context, DataStore.FutureItem item) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("id", item.id);
        intent.putExtra("title", item.title);
        return PendingIntent.getBroadcast(
                context,
                requestCode(item.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static int requestCode(long id) {
        return (int) (id ^ (id >>> 32));
    }
}
